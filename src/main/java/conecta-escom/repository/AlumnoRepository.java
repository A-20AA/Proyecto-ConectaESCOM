package conecta_escom.repository;

import conecta_escom.model.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {
    Alumno findByBoleta(String boleta);
    boolean existsByBoleta(String boleta);
    boolean existsByCorreo(String correo);
}
