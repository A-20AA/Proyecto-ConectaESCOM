package conecta_escom.controller;

import conecta_escom.model.User;
import conecta_escom.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // --- ROOT --- 
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // --- LOGIN ---
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        User user = userRepository.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user); // Guardamos el usuario completo en sesión
            return "redirect:/home";
        } else {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }
    }

    // --- REGISTER ---
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String nombre,
                           @RequestParam String username,
                           @RequestParam String correo,
                           @RequestParam String password,  
                           Model model) {

        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "El usuario ya está registrado");
            return "register";
        }

        User user = new User();
        user.setNombre(nombre);
        user.setUsername(username);
        user.setCorreo(correo);		
        user.setPassword(password); 
        userRepository.save(user);

        return "redirect:/login";
    }

    // --- HOME ---
    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); // Recuperamos el usuario de la sesión
        if (user == null) {
            return "redirect:/login"; // Redirige si no hay sesión
        }
        model.addAttribute("user", user);
        return "home";
    }

    // --- LOGOUT ---
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destruye la sesión
        return "redirect:/login";
    }
}
