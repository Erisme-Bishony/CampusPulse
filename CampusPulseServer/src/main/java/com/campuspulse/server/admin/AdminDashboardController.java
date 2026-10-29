package com.campuspulse.server.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.BehaviorRepository;
import com.campuspulse.server.repository.RegistrationRepository;
import com.campuspulse.server.repository.UserRepository;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final BehaviorRepository behaviorRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    ActivityRepository activityRepository,
                                    RegistrationRepository registrationRepository,
                                    BehaviorRepository behaviorRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
        this.behaviorRepository = behaviorRepository;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = LocalDate.now().atStartOfDay();

        model.addAttribute("active", "dashboard");
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("newUsersToday", userRepository.countByCreatedAtAfter(today));
        model.addAttribute("activityCount", activityRepository.count());
        model.addAttribute("publishedCount", activityRepository.countByStatus("PUBLISHED"));
        model.addAttribute("upcomingCount",
                activityRepository.countByStatusAndStartTimeAfter("PUBLISHED", now));
        model.addAttribute("registrationCount", registrationRepository.count());
        model.addAttribute("registeredCount", registrationRepository.countByStatus("REGISTERED"));
        model.addAttribute("registrationsToday", registrationRepository.countByCreatedAtAfter(today));
        model.addAttribute("behaviorCount", behaviorRepository.count());
        model.addAttribute("behaviorsToday", behaviorRepository.countByCreatedAtAfter(today));
        model.addAttribute("recentActivities", activityRepository.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("upcoming", activityRepository
                .findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc("PUBLISHED", now));
        return "admin/dashboard";
    }
}
