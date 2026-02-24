package conecta_escom.controller;

import conecta_escom.model.Alumno;
import conecta_escom.model.Administrador;
import conecta_escom.model.Coordinador;
import conecta_escom.repository.AlumnoRepository;
import conecta_escom.repository.AdministradorRepository;
import conecta_escom.repository.CoordinadorRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private CoordinadorRepository coordinadorRepository;

    @Autowired
    private AdministradorRepository administradorRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Iniciar sesión 
    @PostMapping("/login")
    public String login(@RequestParam String usuario, @RequestParam String password, HttpSession session, Model model) {

        Alumno alumno = alumnoRepository.findByBoleta(usuario);
        if (alumno != null && passwordEncoder.matches(password, alumno.getPassword())) {
            session.setAttribute("usuario", alumno); 
            session.setAttribute("rol", "ALUMNO");
            return "redirect:/alumno/inicio";
        } 

        Coordinador coordinador = coordinadorRepository.findByUsuario(usuario);
        if (coordinador != null && passwordEncoder.matches(password, coordinador.getPassword())) {
            session.setAttribute("usuario", coordinador);
            session.setAttribute("rol", "COORDINADOR");
            return "redirect:/coordinador/inicio";
        }

        Administrador administrador = administradorRepository.findByUsuario(usuario);
        if (administrador != null && passwordEncoder.matches(password, administrador.getPassword())) {
            session.setAttribute("usuario", administrador);
            session.setAttribute("rol", "ADMINISTRADOR");
            return "redirect:/administrador/inicio";
        }

        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "login";
    }

    // Cerrar sesión
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destruye la sesión
        return "redirect:/login";
    }
}
