package edu.tlu.klgd.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "class_record")
public class ClassRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;

    @Column(name = "academic_year")
    private String academicYear;
    @Column(name = "semester")
    private String semester;
    @Column(name = "class_name")
    private String className;
    @Column(name = "subject_name")
    private String subjectName;
    @Column(name = "credits")
    private Double credits;
    @Column(name = "department_hn")
    private String departmentHn;
    @Column(name = "department_ph")
    private String departmentPh;
    @Column(name = "student_count")
    private Integer studentCount;
    @Column(name = "teacher_original_name")
    private String teacherOriginalName;
    @Column(name = "teacher_name")
    private String teacherName;
    @Column(name = "position")
    private String position;
    @Column(name = "degree")
    private String degree;
    @Column(name = "academic_title")
    private String academicTitle;
    @Column(name = "unit_name")
    private String unitName;
    @Column(name = "source_row_number")
    private Integer sourceRowNumber;
    @Column(name = "valid", nullable = false)
    private boolean valid = true;
    @Column(name = "warning_message", length = 1000)
    private String warningMessage;

    public Long getId() { return id; }
    public ImportBatch getImportBatch() { return importBatch; }
    public void setImportBatch(ImportBatch importBatch) { this.importBatch = importBatch; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Double getCredits() { return credits; }
    public void setCredits(Double credits) { this.credits = credits; }
    public String getDepartmentHn() { return departmentHn; }
    public void setDepartmentHn(String departmentHn) { this.departmentHn = departmentHn; }
    public String getDepartmentPh() { return departmentPh; }
    public void setDepartmentPh(String departmentPh) { this.departmentPh = departmentPh; }
    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
    public String getTeacherOriginalName() { return teacherOriginalName; }
    public void setTeacherOriginalName(String teacherOriginalName) { this.teacherOriginalName = teacherOriginalName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getAcademicTitle() { return academicTitle; }
    public void setAcademicTitle(String academicTitle) { this.academicTitle = academicTitle; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}
