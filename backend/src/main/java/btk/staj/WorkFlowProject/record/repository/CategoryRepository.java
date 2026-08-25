package btk.staj.WorkFlowProject.record.repository;

import btk.staj.WorkFlowProject.record.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // Kategori ismine göre arama yapmak ileride gerekebileceği için 
    // bu metodu baştan eklemek iyi bir mimari yaklaşımdır.
    Optional<Category> findByName(String name);
    
}