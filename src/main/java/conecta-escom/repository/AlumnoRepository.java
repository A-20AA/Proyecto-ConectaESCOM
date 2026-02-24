package conecta_escom.repository;

import conecta_escom.model.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {
    Alumno findByBoleta(String boleta);
    boolean existsByBoleta(String boleta);
    boolean existsByCorreo(String correo);

    @Query("SELECT a FROM Alumno a LEFT JOIN FETCH a.clubesInscritos WHERE a.id = :id")
    Alumno findByIdWithClubs(@Param("id") Integer id);
}
