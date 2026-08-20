package com.civic.reporting.ai;

import com.civic.reporting.entity.AIClassification;
import com.civic.reporting.entity.Department;
import com.civic.reporting.entity.Issue;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.repository.AIClassificationRepository;
import com.civic.reporting.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * High-performance central AI Orchestration Engine for the Civic Issue Platform.
 * Coordinates NLP classification, risk/urgency scoring, duplicate detection,
 * resolution time prediction, and image validation.
 */
@Service
public class CivicAiEngine {

    private static final Logger log = LoggerFactory.getLogger(CivicAiEngine.class);
    private static final String MODEL_VERSION = "CivicAI-v2.1-Hybrid";

    private final NlpTextClassifier textClassifier;
    private final UrgencyRiskModel urgencyRiskModel;
    private final DuplicateDetectionEngine duplicateDetector;
    private final ResolutionPredictor resolutionPredictor;
    private final ImageQualityValidator imageValidator;
    private final AIClassificationRepository aiClassificationRepository;
    private final DepartmentRepository departmentRepository;

    public CivicAiEngine(NlpTextClassifier textClassifier,
                           UrgencyRiskModel urgencyRiskModel,
                           DuplicateDetectionEngine duplicateDetector,
                           ResolutionPredictor resolutionPredictor,
                           ImageQualityValidator imageValidator,
                           AIClassificationRepository aiClassificationRepository,
                           DepartmentRepository departmentRepository) {
        this.textClassifier = textClassifier;
        this.urgencyRiskModel = urgencyRiskModel;
        this.duplicateDetector = duplicateDetector;
        this.resolutionPredictor = resolutionPredictor;
        this.imageValidator = imageValidator;
        this.aiClassificationRepository = aiClassificationRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Executes the complete AI pipeline on raw complaint inputs.
     */
    public AiAnalysisResult analyze(String title,
                                   String description,
                                   Double latitude,
                                   Double longitude,
                                   String address,
                                   IssueCategory citizenSelectedCategory,
                                   Long departmentId,
                                   MultipartFile imageFile) {
        long startTime = System.currentTimeMillis();

        // 1. NLP Text Classification
        NlpTextClassifier.ClassificationOutput nlpOutput = textClassifier.classify(title, description);

        // Determine effective category
        IssueCategory effectiveCategory;
        if (citizenSelectedCategory != null && citizenSelectedCategory != IssueCategory.OTHER) {
            effectiveCategory = citizenSelectedCategory;
        } else if (nlpOutput.getCategory() != null && nlpOutput.getCategory() != IssueCategory.OTHER) {
            effectiveCategory = nlpOutput.getCategory();
        } else {
            effectiveCategory = IssueCategory.OTHER;
        }

        // 2. Urgency & Safety Hazard Scoring
        UrgencyRiskModel.UrgencyEvaluation urgencyEval = urgencyRiskModel.evaluate(effectiveCategory, title, description);

        // 3. Duplicate Complaint Detection
        DuplicateDetectionEngine.DuplicateCheckResult dupCheck = duplicateDetector.findDuplicate(
                effectiveCategory, title, description, latitude, longitude, address
        );

        // 4. Resolve Department ID if not provided
        Long targetDeptId = departmentId;
        if (targetDeptId == null && effectiveCategory != IssueCategory.OTHER) {
            String deptCode = mapCategoryToDepartmentCode(effectiveCategory);
            Optional<Department> deptOpt = departmentRepository.findByCodeIgnoreCase(deptCode);
            if (deptOpt.isPresent()) {
                targetDeptId = deptOpt.get().getId();
            }
        }

        // 5. Resolution Turnaround Time Prediction (ETA)
        int estimatedHours = resolutionPredictor.predictResolutionHours(
                effectiveCategory, urgencyEval.getPriority(), targetDeptId
        );

        // 6. Image Quality & Evidence Validation
        ImageQualityValidator.ImageValidationResult imgResult = imageValidator.validateAndAnalyze(imageFile);

        long inferenceTimeMs = Math.max(1, System.currentTimeMillis() - startTime);

        // 7. Assemble Unified Result
        AiAnalysisResult result = new AiAnalysisResult();
        result.setPredictedCategory(nlpOutput.getCategory());
        result.setConfidenceScore(nlpOutput.getConfidence());
        result.setCategoryProbabilities(nlpOutput.getProbabilities());
        result.setPriority(urgencyEval.getPriority());
        result.setUrgencyScore(urgencyEval.getUrgencyScore());
        result.setEstimatedResolutionHours(estimatedHours);
        result.setDuplicate(dupCheck.isDuplicate());
        result.setDuplicateOfTrackingNumber(dupCheck.getDuplicateOfTrackingNumber());
        result.setDuplicateSimilarity(dupCheck.getSimilarityScore());
        result.setModelVersion(MODEL_VERSION);
        result.setInferenceTimeMs(inferenceTimeMs);

        String rationale = urgencyEval.getRationale();
        if (dupCheck.isDuplicate()) {
            rationale += " [Alert: Potential duplicate of " + dupCheck.getDuplicateOfTrackingNumber() + " with "
                    + (int)(dupCheck.getSimilarityScore() * 100) + "% similarity].";
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            rationale += " [" + imgResult.getSummary() + "].";
        }
        result.setRationale(rationale);

        return result;
    }

    /**
     * Persists the AI classification record linked to an existing Issue entity.
     */
    public AIClassification recordClassification(Issue issue, AiAnalysisResult aiResult) {
        String rawOutput = "{\"priority\":\"" + aiResult.getPriority().name() + "\",\"urgencyScore\":" + aiResult.getUrgencyScore()
                + ",\"isDuplicate\":" + aiResult.isDuplicate() + ",\"duplicateOf\":\"" + (aiResult.getDuplicateOfTrackingNumber() != null ? aiResult.getDuplicateOfTrackingNumber() : "") + "\""
                + ",\"estimatedHours\":" + aiResult.getEstimatedResolutionHours() + ",\"probabilities\":" + aiResult.getCategoryProbabilities() + "}";

        AIClassification record = new AIClassification(
                issue,
                aiResult.getPredictedCategory() != null ? aiResult.getPredictedCategory().name() : IssueCategory.OTHER.name(),
                aiResult.getConfidenceScore(),
                aiResult.getModelVersion(),
                rawOutput,
                aiResult.getInferenceTimeMs()
        );

        return aiClassificationRepository.save(record);
    }

    public String mapCategoryToDepartmentCode(IssueCategory category) {
        if (category == null) return "ROADS";
        return switch (category) {
            case ROADS -> "ROADS";
            case ELECTRICITY -> "ELECTRICITY";
            case GARBAGE_SANITATION -> "SANITATION";
            case WATER -> "WATER";
            case DRAINAGE -> "DRAINAGE";
            default -> "ROADS";
        };
    }
}
