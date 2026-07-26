package com.simpleRuleEngine.mapper;

import com.simpleRuleEngine.dto.request.BusinessRuleCreateRequest;
import com.simpleRuleEngine.dto.request.BusinessRuleUpdateRequest;
import com.simpleRuleEngine.dto.response.BusinessRuleResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface BusinessRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ruleCode", ignore = true)
    BusinessRule toEntity(BusinessRuleCreateRequest request);

    BusinessRuleResponse toResponse(BusinessRule rule);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ruleCode", ignore = true)
    void updateEntity(BusinessRuleUpdateRequest request, @MappingTarget BusinessRule rule);
}
