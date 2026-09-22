package com.JavaTraining.BaiTap_RS.bootstrap;

import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import org.springframework.stereotype.Component;

@Component
public class DemoTimetableCatalogGateway {

    private final ClassSubjectRepository classSubjectRepo;
    private final SubjectRepository subjectRepo;
    private final SubjectTeachingAssignmentRepository assignmentRepo;
    private final FunctionalRoomRepository roomRepo;
    private final SubjectFunctionalRoomRepository subjectFunctionalRoomRepo;

    public DemoTimetableCatalogGateway(
            ClassSubjectRepository classSubjectRepository,
            SubjectRepository subjectRepository,
            SubjectTeachingAssignmentRepository assignmentRepository,
            FunctionalRoomRepository roomRepository,
            SubjectFunctionalRoomRepository subjectFunctionalRoomRepository) {
        this.classSubjectRepo = classSubjectRepository;
        this.subjectRepo = subjectRepository;
        this.assignmentRepo = assignmentRepository;
        this.roomRepo = roomRepository;
        this.subjectFunctionalRoomRepo = subjectFunctionalRoomRepository;
    }

    public ClassSubjectRepository classSubjectRepository() {
        return classSubjectRepo;
    }

    public SubjectRepository subjectRepository() {
        return subjectRepo;
    }

    public SubjectTeachingAssignmentRepository assignmentRepository() {
        return assignmentRepo;
    }

    public FunctionalRoomRepository roomRepository() {
        return roomRepo;
    }

    public SubjectFunctionalRoomRepository subjectFunctionalRoomRepository() {
        return subjectFunctionalRoomRepo;
    }
}
