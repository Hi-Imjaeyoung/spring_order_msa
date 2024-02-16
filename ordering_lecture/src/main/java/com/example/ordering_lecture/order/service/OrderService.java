package com.example.ordering_lecture.order.service;

import com.example.ordering_lecture.item.domain.Item;
import com.example.ordering_lecture.item.repository.ItemRepository;
import com.example.ordering_lecture.member.domain.Member;
import com.example.ordering_lecture.member.repository.MemberRepository;
import com.example.ordering_lecture.order.domain.OrderStatus;
import com.example.ordering_lecture.order.domain.Ordering;
import com.example.ordering_lecture.order.dto.OrderReqDto;
import com.example.ordering_lecture.order.dto.OrderResDto;
import com.example.ordering_lecture.order.repository.OrderRepository;
import com.example.ordering_lecture.order_item.domain.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    @Autowired
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    public OrderService(OrderRepository orderRepository, MemberRepository memberRepository, ItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
        this.itemRepository = itemRepository;
    }

    public Ordering create(List<OrderReqDto> orderReqDtos) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = memberRepository.findByEmail(authentication.getName()).orElseThrow(()-> new EntityNotFoundException("그런 멤버는 없습니다."));
        Ordering ordering = Ordering.builder().member(member).build();
        for(OrderReqDto now : orderReqDtos){
            Item item = itemRepository.findById(now.getItemId()).orElseThrow(()->new EntityNotFoundException("그런 아이템은 없습니다."));
            OrderItem orderItem = OrderItem.builder()
                    .item(item)
                    .quantity(now.getCount())
                    .ordering(ordering)
                    .build();
            ordering.getOrderItemList().add(orderItem);
            if(item.getStockQuantity() < orderItem.getQuantity()){
                throw new IllegalArgumentException("아이템의 갯수보다 많이 주문할 수 없습니다.");
            }
            orderItem.getItem().updateStockQuantity(Math.toIntExact(now.getCount()));
        }
        return orderRepository.save(ordering);
    }

    public Ordering cancel(Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Ordering ordering =orderRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("없는 주문 입니다."));
        if(ordering.getOrderStatus().equals(OrderStatus.CANCELED)){
            throw new IllegalArgumentException("이미 취소된 주문입니다.");
        }

        if(ordering.getMember().getEmail().equals(email) || authentication.getAuthorities().contains((new SimpleGrantedAuthority("ROLE_ADMIN")))){
            ordering.cancelOder();
            for(OrderItem now : ordering.getOrderItemList()){
                Item item = itemRepository.findById(now.getItem().getId()).orElseThrow(()-> new EntityNotFoundException("해당 아이템이 없습니다"));
                item.updateStockQuantity((-1*now.getQuantity().intValue()));
            }
            return ordering;
        }
        throw new AccessDeniedException("주문을 취소할 권한이 없습니다.");
    }

    public List<OrderResDto> findAll() {
        List<OrderResDto> orderResDtos = new ArrayList<>();
        return orderRepository.findAll().stream().map(a->OrderResDto.toDto(a)).collect(Collectors.toList());
    }

    public List<OrderResDto> findMyOrder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return memberRepository.findByEmail(email).orElseThrow(()-> new AccessDeniedException("잘못된 접근입니다."))
                .getOrderings()
                .stream()
                .map(a->OrderResDto.toDto(a))
                .collect(Collectors.toList());
    }
    public List<OrderResDto> findMyOrder(Long id) {
        // order레포에서 바로 memberID 로 조회하는 방법도 있다. (바로 가져오기)
//        orderRepository.findByMemberId(id);
        return memberRepository.findById(id).orElseThrow(()-> new AccessDeniedException("잘못된 접근입니다."))
                .getOrderings()
                .stream()
                .map(a->OrderResDto.toDto(a))
                .collect(Collectors.toList());
    }
}
