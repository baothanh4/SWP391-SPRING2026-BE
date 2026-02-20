package com.example.SWP391_SPRING2026.Service;


import com.example.SWP391_SPRING2026.DTO.Request.AddressRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.AddressUpdateDTO;
import com.example.SWP391_SPRING2026.DTO.Response.AddressResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Address;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Repository.AddressRepository;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import com.example.SWP391_SPRING2026.mapper.AddressMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressResponseDTO createAddress(Long userId, AddressRequestDTO dto){
        Users user=userRepository.findById(userId).orElseThrow(()->new RuntimeException("User not found"));

        boolean hasAddress = !addressRepository.findByUserId(userId).isEmpty();

        Address address=new Address();
        address.setUser(user);
        address.setReceiverName(dto.getReceiverName());
        address.setPhone(dto.getPhone());
        address.setAddressLine(dto.getAddressLine());
        address.setWard(dto.getWard());
        address.setDistrict(dto.getDistrict());
        address.setProvince(dto.getProvince());
        address.setDistrictId(dto.getDistrictId());
        address.setWardCode(dto.getWardCode());
        address.setIsDefault(!hasAddress);

        return AddressMapper.toDTO(addressRepository.save(address));
    }

    public List<AddressResponseDTO> getAll(Long userId){
        return addressRepository.findByUserId(userId)
                .stream()
                .map(AddressMapper::toDTO)
                .toList();
    }

    public AddressResponseDTO updateInfo(Long userId, Long addressId, AddressUpdateDTO dto){
        Address address=getOwnedAddress(userId,addressId);

        if(dto.getReceiverName()!=null){
            address.setReceiverName(dto.getReceiverName());
        }

        if(dto.getPhone()!=null){
            address.setPhone(dto.getPhone());
        }

        if(dto.getAddressLine()!=null){
            address.setAddressLine(dto.getAddressLine());
        }

        if(dto.getWard()!=null){
            address.setWard(dto.getWard());
        }

        if(dto.getDistrict()!=null){
            address.setDistrict(dto.getDistrict());
        }

        if(dto.getProvince()!=null){
            address.setProvince(dto.getProvince());
        }

        if(dto.getDistrictId()!=null){
            address.setDistrictId(dto.getDistrictId());
        }

        if(dto.getWardCode()!=null){
            address.setWardCode(dto.getWardCode());
        }

        return AddressMapper.toDTO(addressRepository.save(address));
    }

    @Transactional
    public void setDefault(Long userId,Long addressId){
        Address address=getOwnedAddress(userId,addressId);

        unsetDefault(userId);
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    public void delete(Long userId,Long addressId){
        addressRepository.deleteByIdAndUserId(addressId,userId);
    }

    private Address getOwnedAddress(Long userId,Long addressId){
        Address address=addressRepository.findById(addressId).orElseThrow(()-> new RuntimeException("Address not found"));

        if(!address.getUser().getId().equals(userId)){
            throw new RuntimeException("User not owned");
        }
        return address;
    }

    @Transactional
    protected void unsetDefault(Long userId){
        addressRepository.findByUserId(userId).forEach(address -> {
            if(Boolean.TRUE.equals(address.getIsDefault())){
                address.setIsDefault(false);
                addressRepository.save(address);
            }
        });
    }
}
