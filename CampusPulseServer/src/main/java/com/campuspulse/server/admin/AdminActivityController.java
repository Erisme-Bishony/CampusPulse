package com.campuspulse.server.admin;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.TagRepository;

@Controller
@RequestMapping("/admin/activities")
public class AdminActivityController {

    private static final List<String> TYPES = List.of("讲座", "比赛", "文艺", "志愿", "体育", "社团", "其他");

    private final ActivityRepository activityRepository;
    private final TagRepository tagRepository;
    private final AdminActivityService activityService;

    public AdminActivityController(ActivityRepository activityRepository,
                                   TagRepository tagRepository,
                                   AdminActivityService activityService) {
        this.activityRepository = activityRepository;
        this.tagRepository = tagRepository;
        this.activityService = activityService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("active", "activities");
        model.addAttribute("activities",
                activityRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "admin/activities";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("active", "activities");
        model.addAttribute("form", new AdminActivityForm());
        model.addAttribute("tags", tagRepository.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("types", TYPES);
        model.addAttribute("editing", false);
        return "admin/activity-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Activity activity = activityRepository.findById(id)
                .orElse(null);
        if (activity == null) {
            return "redirect:/admin/activities";
        }
        model.addAttribute("active", "activities");
        model.addAttribute("form", AdminActivityForm.from(activity, activityService.tagIdsOf(id)));
        model.addAttribute("tags", tagRepository.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("types", TYPES);
        model.addAttribute("editing", true);
        return "admin/activity-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AdminActivityForm form,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            activityService.save(form);
            redirectAttributes.addFlashAttribute("message",
                    form.getId() == null ? "活动已创建" : "活动已保存");
            return "redirect:/admin/activities";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("active", "activities");
            model.addAttribute("form", form);
            model.addAttribute("tags", tagRepository.findAllByOrderBySortOrderAscIdAsc());
            model.addAttribute("types", TYPES);
            model.addAttribute("editing", form.getId() != null);
            model.addAttribute("error", ex.getMessage());
            return "admin/activity-form";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            activityService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("message", "活动状态已更新");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/activities";
    }
}
