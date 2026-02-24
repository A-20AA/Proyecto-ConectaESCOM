package conecta_escom.repository;

import conecta_escom.model.Club;
import conecta_escom.model.Coordinador;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Integer> {
    Club findByClave(String clave);
    List<Club> findByCoordinadorClub(Coordinador coordinador);
}
