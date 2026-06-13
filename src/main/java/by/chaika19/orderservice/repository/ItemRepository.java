package by.chaika19.orderservice.repository;

import by.chaika19.orderservice.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository <Item, Long> { }
