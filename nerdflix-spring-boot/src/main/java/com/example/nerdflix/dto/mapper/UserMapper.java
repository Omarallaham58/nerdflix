package com.example.nerdflix.dto.mapper;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;


import com.example.nerdflix.dto.request.CreateUserRequest;
import com.example.nerdflix.dto.response.UserDto;
import com.example.nerdflix.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

//@Mapper(componentModel="spring")
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(
            target = "roles",
            expression =
                    "java(user.getRoles().stream()"
                            + ".map(com.example.nerdflix.entity.Role::getName)"
                            + ".toList())"
    )
    UserDto toDto(User user);


}
