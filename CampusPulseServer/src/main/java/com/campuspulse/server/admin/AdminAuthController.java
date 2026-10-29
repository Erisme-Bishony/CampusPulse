package com.campuspulse.server.admin;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.campuspulse.server.domain.Admin;
import com.campuspulse.server.repository.AdminRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        if (AdminSession.isLoggedIn(request.getSession(false))) {
            return "redirect:/admin";
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {
        Optional<Admin> found = adminRepository.findByUsername(username == null ? "" : username.trim());
        if (found.isEmpty()
                || !passwordEncoder.matches(password == null ? "" : password, found.get().getPasswordHash())
                || found.get().getStatus() == null
                || found.get().getStatus().intValue() != 1) {
            model.addAttribute("error", "用户名或密码错误，或账号已禁用");
            model.addAttribute("username", username);
            return "admin/login";
        }

        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute(AdminSession.ADMIN_ID, found.get().getId());
        session.setAttribute(AdminSession.ADMIN_NAME, found.get().getDisplayName());
        AdminSession.ensureCsrfToken(session);
        return "redirect:/admin";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/admin/login";
    }
}
