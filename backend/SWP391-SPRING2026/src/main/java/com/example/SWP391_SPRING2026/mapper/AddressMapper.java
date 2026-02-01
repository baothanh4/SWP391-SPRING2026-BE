package com.example.SWP391_SPRING2026.mapper;

import com.example.SWP391_SPRING2026.DTO.Request.AddressRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.AddressResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Address;
import org.springframework.beans.BeanUtils;

public class AddressMapper {
    public static AddressResponseDTO toDTO(Address address) {
        AddressResponseDTO addressResponseDTO = new AddressResponseDTO();
        BeanUtils.copyProperties(address,addressResponseDTO);
        return addressResponseDTO;
    }
}
