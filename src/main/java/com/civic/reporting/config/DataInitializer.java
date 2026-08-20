package com.civic.reporting.config;

import com.civic.reporting.entity.Department;
import com.civic.reporting.entity.Issue;
import com.civic.reporting.entity.IssueUpdate;
import com.civic.reporting.entity.User;
import com.civic.reporting.enums.IssueCategory;
import com.civic.reporting.enums.IssueStatus;
import com.civic.reporting.enums.UserRole;
import com.civic.reporting.repository.DepartmentRepository;
import com.civic.reporting.repository.IssueRepository;
import com.civic.reporting.repository.IssueUpdateRepository;
import com.civic.reporting.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final IssueUpdateRepository issueUpdateRepository;

    public DataInitializer(DepartmentRepository departmentRepository,
                           UserRepository userRepository,
                           IssueRepository issueRepository,
                           IssueUpdateRepository issueUpdateRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.issueUpdateRepository = issueUpdateRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Handle legacy admin email migration if applicable
        userRepository.findByEmail("admin@civic-portal.gov").ifPresent(oldAdmin -> {
            if (userRepository.findByEmail("admin@civic.gov").isEmpty()) {
                oldAdmin.setEmail("admin@civic.gov");
                if (oldAdmin.getPassword() == null || oldAdmin.getPassword().isEmpty()) {
                    oldAdmin.setPassword("admin123");
                }
                userRepository.save(oldAdmin);
            }
        });

        // 2. Ensure Admin user exists with valid credentials
        getOrCreateUser("Municipal Administrator", "admin@civic.gov", "admin123", "+1-800-555-0001", UserRole.ADMIN, null);

        // 3. Ensure all officer accounts have default passwords
        for (User u : userRepository.findAll()) {
            if (u.getRole() == UserRole.OFFICER && (u.getPassword() == null || u.getPassword().isEmpty())) {
                u.setPassword("officer123");
                userRepository.save(u);
            }
        }

        if (departmentRepository.count() > 0 && issueRepository.count() > 0) {
            log.info("Database already initialized with municipal data.");
            return;
        }

        log.info("Seeding initial municipal departments, officers, and baseline civic issues...");

        // 4. Seed Departments
        Department roadDept = getOrCreateDepartment(
                "ROADS",
                "Road Department",
                "Responsible for maintenance of municipal roads, asphalt resurfacing, pothole repairs, and traffic signage.",
                "roads@civic-portal.gov",
                "+1-800-555-ROAD"
        );

        Department elecDept = getOrCreateDepartment(
                "ELECTRICITY",
                "Electricity Department",
                "Maintains public streetlighting, electrical transformers, traffic lights, and municipal power distribution.",
                "electricity@civic-portal.gov",
                "+1-800-555-ELEC"
        );

        Department sanDept = getOrCreateDepartment(
                "SANITATION",
                "Sanitation/Garbage Department",
                "Manages municipal solid waste collection, recycling centers, street sweeping, and public garbage bin clearance.",
                "sanitation@civic-portal.gov",
                "+1-800-555-WSTE"
        );

        Department waterDept = getOrCreateDepartment(
                "WATER",
                "Water Department",
                "Oversees municipal drinking water supply, pipeline networks, pump stations, and residential water connections.",
                "water@civic-portal.gov",
                "+1-800-555-WATR"
        );

        Department drainDept = getOrCreateDepartment(
                "DRAINAGE",
                "Drainage Department",
                "Maintains stormwater drains, underground sewage lines, flood mitigation pumps, and open canal desilting.",
                "drainage@civic-portal.gov",
                "+1-800-555-DRAN"
        );

        // 5. Seed Officers & Citizens
        User roadOfficer = getOrCreateUser("Marcus Vance (Roads)", "officer.road@civic.gov", "officer123", "+1-800-555-0101", UserRole.OFFICER, roadDept);
        User elecOfficer = getOrCreateUser("Elena Ramos (Electricity)", "officer.electricity@civic.gov", "officer123", "+1-800-555-0102", UserRole.OFFICER, elecDept);
        User sanOfficer = getOrCreateUser("David Chen (Sanitation)", "officer.sanitation@civic.gov", "officer123", "+1-800-555-0103", UserRole.OFFICER, sanDept);
        User waterOfficer = getOrCreateUser("Sarah Jenkins (Water)", "officer.water@civic.gov", "officer123", "+1-800-555-0104", UserRole.OFFICER, waterDept);
        User drainOfficer = getOrCreateUser("Carlos Gomez (Drainage)", "officer.drainage@civic.gov", "officer123", "+1-800-555-0105", UserRole.OFFICER, drainDept);

        User citizen1 = getOrCreateUser("Aarav Sharma", "aarav.sharma@example.com", "citizen123", "+1-555-0199", UserRole.CITIZEN, null);
        User citizen2 = getOrCreateUser("Maya Patel", "maya.patel@example.com", "citizen123", "+1-555-0188", UserRole.CITIZEN, null);

        // 6. Seed Sample Issues & Audit Updates
        if (issueRepository.count() == 0) {
            // Issue 1: Pothole on Main Boulevard
            Issue issue1 = new Issue();
            issue1.setTrackingNumber("CIV-2026-00124");
            issue1.setTitle("Pothole on Main Boulevard");
            issue1.setDescription("Deep pothole in the center lane causing dangerous vehicle swerves.");
            issue1.setCategory(IssueCategory.ROADS);
            issue1.setStatus(IssueStatus.IN_PROGRESS);
            issue1.setLatitude(37.7749);
            issue1.setLongitude(-122.4194);
            issue1.setAddress("450 Market St, Sector 4");
            issue1.setCitizen(citizen1);
            issue1.setAssignedDepartment(roadDept);
            issue1.setAssignedOfficer(roadOfficer);
            issue1.setAiConfidence(0.94);
            issue1.setAiSuggestedCategory("ROADS");
            issue1 = issueRepository.save(issue1);

            issueUpdateRepository.save(new IssueUpdate(
                    issue1, citizen1, null, IssueStatus.REPORTED, "INITIAL_REPORT", "Reported by citizen with mobile GPS coordinates."
            ));
            issueUpdateRepository.save(new IssueUpdate(
                    issue1, null, IssueStatus.REPORTED, IssueStatus.AI_CLASSIFIED, "AI_CLASSIFICATION", "Classified as Road Damage / Pothole (Confidence: 96.0%). Routed to Road Department."
            ));
            issueUpdateRepository.save(new IssueUpdate(
                    issue1, roadOfficer, IssueStatus.AI_CLASSIFIED, IssueStatus.IN_PROGRESS, "STATUS_CHANGE", "Road repair crew dispatched with asphalt patcher."
            ));

            // Issue 2: Garbage Overflow in Community Park
            Issue issue2 = new Issue();
            issue2.setTrackingNumber("CIV-2026-00125");
            issue2.setTitle("Uncollected Garbage Dump Overflowing at Lincoln Park");
            issue2.setDescription("Commercial bins overflowing with waste for the past 3 days, creating odor.");
            issue2.setCategory(IssueCategory.GARBAGE_SANITATION);
            issue2.setStatus(IssueStatus.ASSIGNED);
            issue2.setLatitude(37.7833);
            issue2.setLongitude(-122.4167);
            issue2.setAddress("Lincoln Park North Entrance, 14th Ave");
            issue2.setCitizen(citizen2);
            issue2.setAssignedDepartment(sanDept);
            issue2.setAssignedOfficer(sanOfficer);
            issue2.setAiConfidence(0.91);
            issue2.setAiSuggestedCategory("GARBAGE_SANITATION");
            issue2 = issueRepository.save(issue2);

            issueUpdateRepository.save(new IssueUpdate(
                    issue2, citizen2, null, IssueStatus.REPORTED, "INITIAL_REPORT", "Citizen submitted report with photo."
            ));
            issueUpdateRepository.save(new IssueUpdate(
                    issue2, null, IssueStatus.REPORTED, IssueStatus.ASSIGNED, "DEPARTMENT_ASSIGNMENT", "Assigned to Sanitation/Garbage Department for morning route pickup."
            ));

            // Issue 3: Streetlight Malfunction
            Issue issue3 = new Issue();
            issue3.setTrackingNumber("CIV-2026-00126");
            issue3.setTitle("Flickering Streetlights on Oakwood Avenue");
            issue3.setDescription("Three street poles are dark, making pedestrian crossing unsafe after dusk.");
            issue3.setCategory(IssueCategory.ELECTRICITY);
            issue3.setStatus(IssueStatus.RESOLVED);
            issue3.setLatitude(37.7690);
            issue3.setLongitude(-122.4467);
            issue3.setAddress("780 Oakwood Ave & 19th St");
            issue3.setCitizen(citizen1);
            issue3.setAssignedDepartment(elecDept);
            issue3.setAssignedOfficer(elecOfficer);
            issue3.setAiConfidence(0.88);
            issue3.setAiSuggestedCategory("ELECTRICITY");
            issue3 = issueRepository.save(issue3);

            issueUpdateRepository.save(new IssueUpdate(
                    issue3, citizen1, null, IssueStatus.REPORTED, "INITIAL_REPORT", "Reported by resident."
            ));
            issueUpdateRepository.save(new IssueUpdate(
                    issue3, elecOfficer, IssueStatus.REPORTED, IssueStatus.RESOLVED, "RESOLUTION", "Replaced burnt LED drivers and repaired wiring connection on poles 14-16."
            ));

            // Issue 4: Water Main Leak
            Issue issue4 = new Issue();
            issue4.setTrackingNumber("CIV-2026-00127");
            issue4.setTitle("Water Gushing from Pavement Valve");
            issue4.setDescription("Underground water pipe joint leaking steadily onto the sidewalk.");
            issue4.setCategory(IssueCategory.WATER);
            issue4.setStatus(IssueStatus.REPORTED);
            issue4.setLatitude(37.7550);
            issue4.setLongitude(-122.4200);
            issue4.setAddress("Mission St & 24th St Intersection");
            issue4.setCitizen(citizen2);
            issue4.setAssignedDepartment(waterDept);
            issueRepository.save(issue4);

            issueUpdateRepository.save(new IssueUpdate(
                    issue4, citizen2, null, IssueStatus.REPORTED, "INITIAL_REPORT", "New citizen submission awaiting dispatch."
            ));

            log.info("Data initialization complete! Seeded 5 departments, 8 users, and 4 sample issues with audit logs.");
        } else {
            log.info("Data initialization complete! Municipal data and issues already present.");
        }
    }

    private Department getOrCreateDepartment(String code, String name, String description, String contactEmail, String contactPhone) {
        return departmentRepository.findByCodeIgnoreCase(code).orElseGet(() ->
                departmentRepository.save(new Department(code, name, description, contactEmail, contactPhone))
        );
    }

    private User getOrCreateUser(String name, String email, String password, String phone, UserRole role, Department department) {
        return userRepository.findByEmail(email).map(existing -> {
            boolean changed = false;
            if (password != null && (existing.getPassword() == null || existing.getPassword().isEmpty())) {
                existing.setPassword(password);
                changed = true;
            }
            if (department != null && existing.getDepartment() == null) {
                existing.setDepartment(department);
                changed = true;
            }
            if (role != null && existing.getRole() == null) {
                existing.setRole(role);
                changed = true;
            }
            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            User user = new User(name, email, password, phone, role);
            if (department != null) {
                user.setDepartment(department);
            }
            return userRepository.save(user);
        });
    }
}
