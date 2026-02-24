package conecta_escom.controller;

import conecta_escom.model.Alumno;
import conecta_escom.model.Club;
import conecta_escom.repository.AlumnoRepository;
import conecta_escom.repository.ClubRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private ClubRepository clubRepository;

    @GetMapping("/")
    public String root() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String indexPage(Model model) {
        // Lógica para la página de inicio 
        List<Club> clubes = clubRepository.findAll();
        long totalClubes = clubRepository.count();
        long totalAlumnos = alumnoRepository.count();       
        
        // Agregamos los datos al modelo
        model.addAttribute("clubes", clubes);
        model.addAttribute("totalClubes", totalClubes);
        model.addAttribute("totalAlumnos", totalAlumnos);
        
        return "index";
    }

    @GetMapping("/clubes")
    public String clubesPage(Model model) {
        // Lógica para la página de los clubes
        List<Club> clubes = clubRepository.findAll();
        
        // Agregamos los datos al modelo
        model.addAttribute("clubes", clubes);
      
        return "clubes";
    }
}
