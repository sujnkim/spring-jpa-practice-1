package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    @Test
    public void 상품주문() {
        // given
        Member member = createMember();
        Item item = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        // when
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        // then
        Order getOrder = orderRepository.findOne(orderId);

        assertThat(getOrder.getStatus())
                .as("상품 주문시 상태는 ORDER").isEqualTo(OrderStatus.ORDER);
        assertThat(getOrder.getOrderItems().size())
                .as("주문한 상품 종류 수가 정확해야 한다.").isEqualTo(1);
        assertThat(getOrder.getTotalPrice())
                .as("주문 가격은 가격*수량이다.").isEqualTo(10000 * 2);
        assertThat(item.getStockQuantity())
                .as("주문 수량만큼 재고가 줄어야 한다.").isEqualTo(8);
    }

    @Test
    public void 상품주문_재고수량초과() {
        // given
        Member member = createMember();
        Item item = createBook("시골 JPA", 10000, 10);

        // when
        int orderCount = 11;    //재고보다 많은 수량

        // then
        assertThatThrownBy(() -> orderService.order(member.getId(), item.getId(), orderCount))
                .isInstanceOf(NotEnoughStockException.class);
    }

    @Test
    public void 주문취소() {
        // given
        Member member = createMember();
        Item item = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        // when
        orderService.cancelOrder(orderId);

        // then
        Order getOrder = orderRepository.findOne(orderId);

        assertThat(getOrder.getStatus())
                .as("주문 취소시 상태는 CANCEL이다.").isEqualTo(OrderStatus.CANCEL);
        assertThat(item.getStockQuantity())
                .as("주문이 취소된 상품은 재고가 원상복귀 되어야 한다.").isEqualTo(10);
    }


    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "강가", "123-123"));
        em.persist(member);
        return member;
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

}