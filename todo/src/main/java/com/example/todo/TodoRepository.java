package com.example.todo;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

  // 完了状態でフィルタリング
  List<Todo> findByCompleted(boolean completed);

  // タイトルで部分一致検索
  List<Todo> findByTitleContainingIgnoreCase(String keyword);

  // 期限日が今日以前のもの
  List<Todo> findByDueDateLessThanEqual(LocalDate date);

  // 優先度でソート
  List<Todo> findAllByOrderByPriorityAsc();

  // @Queryアノテーションの例（完了状態 + タイトル部分一致）
  @Query("select t from Todo t where t.completed = :completed and t.title like %:keyword%")
  List<Todo> searchByStatusAndTitle(@Param("completed") boolean completed,
      @Param("keyword") String keyword);
}
