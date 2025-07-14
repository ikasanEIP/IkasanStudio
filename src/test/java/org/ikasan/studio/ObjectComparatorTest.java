package org.ikasan.studio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ObjectComparatorTest {

    // ✅ Embedded class with mixed field types
    static class Employee {
        String name;
        int age;
        BigDecimal salary;
        LocalDate startDate;

        Employee(String name, BigDecimal salary, LocalDate startDate, int age) {
            this.name = name;
            this.salary = salary;
            this.startDate = startDate;
            this.age = age;
        }
    }

    @Test
    void testEqualWithPrimitiveField() {
        Employee a = new Employee("Alice", new BigDecimal("1000.00"), LocalDate.of(2020, 1, 1), 30);
        Employee b = new Employee("alice", new BigDecimal("1000.0"), LocalDate.of(2020, 1, 1), 30);

        List<String> differences = ObjectComparator.compareFieldsExcept(a, b, List.of());
        assertTrue(differences.isEmpty(), "Expected no differences (age, string, salary, date all equal)");
    }

    @Test
    void testDifferentPrimitiveField() {
        Employee a = new Employee("Alice", new BigDecimal("1000.00"), LocalDate.of(2020, 1, 1), 30);
        Employee b = new Employee("Alice", new BigDecimal("1000.00"), LocalDate.of(2020, 1, 1), 35);

        List<String> differences = ObjectComparator.compareFieldsExcept(a, b, List.of());
        assertFalse(differences.isEmpty(), "Expected a difference in age");
        assertTrue(differences.getFirst().contains("age"));
    }

    @Test
    void testExcludePrimitiveField() {
        Employee a = new Employee("Alice", new BigDecimal("1000.00"), LocalDate.of(2020, 1, 1), 30);
        Employee b = new Employee("Alice", new BigDecimal("1000.00"), LocalDate.of(2020, 1, 1), 99);

        List<String> differences = ObjectComparator.compareFieldsExcept(a, b, List.of("age"));
        assertTrue(differences.isEmpty(), "Expected no differences when age is excluded");
    }
}

