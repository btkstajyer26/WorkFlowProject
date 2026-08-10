package btk.staj.WorkFlowProject.record.controller;

import btk.staj.WorkFlowProject.record.dto.CategoryResponse;
import btk.staj.WorkFlowProject.record.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Sistemdeki tüm kategorileri liste halinde döner.
     * Frontend tarafında "Yeni Kayıt" oluştururken açılır menüyü (select/dropdown) doldurmak için kullanılır.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(category -> {
                    CategoryResponse response = new CategoryResponse();
                    response.setId(category.getId());
                    response.setName(category.getName());
                    return response;
                })
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(categories);
    }
}