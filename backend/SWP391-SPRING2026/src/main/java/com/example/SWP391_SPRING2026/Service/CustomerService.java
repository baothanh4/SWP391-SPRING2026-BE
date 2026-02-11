package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ChangePasswordDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CustomerAccountResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CustomerAccountUpdateDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.OrderItems;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.DuplicateResourceException;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;


    public CustomerAccountResponseDTO getProfile(Long userId){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return map(user);
    }

    public CustomerAccountResponseDTO updateProfile(Long userId, CustomerAccountUpdateDTO dto){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(dto.getFullName() != null){
            user.setFullName(dto.getFullName());
        }

        if(dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())){
            if(userRepository.existsByPhone(dto.getPhone())){
                throw new DuplicateResourceException("PHONE_EXISTS", "Phone already exists");
            }
            user.setPhone(dto.getPhone());
        }

        if(dto.getDob() != null){
            user.setDob(dto.getDob());
        }

        if(dto.getGender() != null){
            user.setGender(dto.getGender());
        }

        return map(userRepository.save(user));
    }

    public void changePassword(Long userId, ChangePasswordDTO dto){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())){
            throw new BadRequestException("Old Password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public void disableAccount(Long userId){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void cancelOrderByCustomer(Long userId,Long orderId){
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        if(!order.getAddress().getUser().getId().equals(userId)){
            throw new BadRequestException("You are not allowed to cancel this order");
        }

        if(order.getOrderStatus() !=  OrderStatus.WAITING_CONFIRM){
            throw new BadRequestException("Order cannot be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        if(order.getOrderType() == OrderType.IN_STOCK){
            for(OrderItems item : order.getOrderItems()){
                ProductVariant variant = item.getProductVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            }
        }


        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.CANCELLED);
        }

        // 6️⃣ Update Shipment
        if (order.getShipment() != null) {
            order.getShipment().setStatus(ShipmentStatus.CANCELLED);
        }
    }


    private CustomerAccountResponseDTO map(Users u) {
        return new CustomerAccountResponseDTO(
                u.getId(),
                u.getEmail(),
                u.getPhone(),
                u.getFullName(),
                u.getGender(),
                u.getDob(),
                u.getStatus(),
                u.getCreateAt()
        );
    }
}
