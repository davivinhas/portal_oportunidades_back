package com.example.portal_oportunidades_back.profile.mapper;

import com.example.portal_oportunidades_back.profile.dto.*;
import com.example.portal_oportunidades_back.profile.entity.*;
import java.util.List;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StudentProfileMapper {
    @Mapping(target = "id", source = "student.id")
    @Mapping(target = "name", source = "student.user.name")
    @Mapping(target = "email", source = "student.user.email")
    @Mapping(target = "experiences", source = "experiences")
    StudentProfileResponse toResponse(Student student, List<ProfessionalExperience> experiences);
    ExperienceResponse toResponse(ProfessionalExperience experience);
}
