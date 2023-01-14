package org.ktorm.dsl

import org.junit.Test
import org.ktorm.BaseTest
import kotlin.test.assertEquals

/**
 * Created by vince at Jan 12, 2023.
 */
class WindowFunctionTest : BaseTest() {

    @Test
    fun testWindowFunctions() {
        // for those that are aggregate functions
        val sums = database
            .from(Employees)
            .selectDistinct(Employees.departmentId, sum(Employees.salary).over { partitionBy(Employees.departmentId) })
            .associate { row ->
                Pair(row.getInt(1), row.getLong(2))
            }

        assertEquals(mapOf(1 to 150L, 2 to 300L), sums)

        // for those that are non-aggregate functions
        val ranks = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.departmentId,
                rank().over { partitionBy(Employees.departmentId).orderBy(Employees.salary.desc()) }
            )
            .map { row ->
                Triple(row.getString(1), row.getInt(2), row.getInt(3))
            }

        assertEquals(setOf("vince", "tom"), ranks.filter { it.third == 1 }.map { it.first }.toSet())

        // for those non-aggregate functions that require parameters
        val groups = database
            .from(Employees)
            .select(Employees.id, ntile(2).over { orderBy(Employees.departmentId.asc()) })
            .associate { row ->
                Pair(row.getInt(1), row.getInt(2))
            }

        assertEquals(mapOf(1 to 1, 2 to 1, 3 to 2, 4 to 2), groups)
    }

    @Test
    fun testRowNumber() {
        val results = database
            .from(Employees)
            .select(Employees.name, rowNumber())
            .joinToString { row ->
                "${row.getString(1)}:${row.getInt(2)}"
            }

        assertEquals("vince:1, marry:2, tom:3, penny:4", results)
    }

    @Test
    fun testRank() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, rank().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getInt(3)}"
            }

        assertEquals(setOf("marry:50:1", "vince:100:2", "penny:100:2", "tom:200:4"), results.toSet())
    }

    @Test
    fun testDenseRank() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, denseRank().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getInt(3)}"
            }

        assertEquals(setOf("marry:50:1", "vince:100:2", "penny:100:2", "tom:200:3"), results.toSet())
    }

    @Test
    fun testPercentRank() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, percentRank().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                String.format("%s:%d:%.2f", row.getString(1), row.getLong(2), row.getDouble(3))
            }

        assertEquals(setOf("marry:50:0.00", "vince:100:0.33", "penny:100:0.33", "tom:200:1.00"), results.toSet())
    }

    @Test
    fun testCumeDist() {
        val results = database
            .from(Employees)
            .select(Employees.name, Employees.salary, cumeDist().over { orderBy(Employees.salary.asc()) })
            .map { row ->
                String.format("%s:%d:%.2f", row.getString(1), row.getLong(2), row.getDouble(3))
            }

        assertEquals(setOf("marry:50:0.25", "vince:100:0.75", "penny:100:0.75", "tom:200:1.00"), results.toSet())
    }

    @Test
    fun testLag() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.salary,
                lag(Employees.salary).over { orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getLong(3).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("marry:50:null", "vince:100:50", "penny:100:100", "tom:200:100"), results.toSet())
    }

    @Test
    fun testLagWithDefaultValue() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.salary,
                lag(Employees.salary, 1, -1).over { orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getLong(3).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("marry:50:-1", "vince:100:50", "penny:100:100", "tom:200:100"), results.toSet())
    }

    @Test
    fun testLagWithDefaultValueByExpression() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.salary,
                lag(Employees.salary + 1, 2, Employees.departmentId.toLong()).over { orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getLong(3).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("marry:50:1", "vince:100:1", "penny:100:51", "tom:200:101"), results.toSet())
    }

    @Test
    fun testLead() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.salary,
                lead(Employees.salary).over { orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getLong(3).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("marry:50:100", "vince:100:100", "penny:100:200", "tom:200:null"), results.toSet())
    }

    @Test
    fun testLeadWithDefaultValue() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.salary,
                lead(Employees.salary, 1, -1).over { orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getLong(3).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("marry:50:100", "vince:100:100", "penny:100:200", "tom:200:-1"), results.toSet())
    }

    @Test
    fun testLeadWithDefaultValueByExpression() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                Employees.salary,
                lead(Employees.salary + 1, 2, Employees.departmentId.toLong()).over { orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}:${row.getLong(3).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("marry:50:101", "vince:100:201", "penny:100:2", "tom:200:2"), results.toSet())
    }

    @Test
    fun testFirstValue() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                firstValue(Employees.salary).over { partitionBy(Employees.departmentId).orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:50", "marry:50", "tom:100", "penny:100"), results.toSet())
    }

    @Test
    fun testLastValue() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                lastValue(Employees.salary + 1).over { partitionBy(Employees.departmentId).orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:101", "marry:51", "tom:201", "penny:101"), results.toSet())
    }

    @Test
    fun testNthValue() {
        val results = database
            .from(Employees)
            .select(
                Employees.name,
                nthValue(Employees.salary, 2).over { partitionBy(Employees.departmentId).orderBy(Employees.salary.asc()) }
            )
            .map { row ->
                "${row.getString(1)}:${row.getLong(2).takeUnless { row.wasNull() }}"
            }

        assertEquals(setOf("vince:100", "marry:null", "tom:200", "penny:null"), results.toSet())
    }

    @Test
    fun testNtile() {
        val results = database
            .from(Employees)
            .select(Employees.name, ntile(3).over { orderBy(Employees.salary.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getInt(2)}"
            }

        assertEquals(setOf("marry:1", "vince:1", "penny:2", "tom:3"), results.toSet())
    }

    @Test
    fun testMin() {
        val results = database
            .from(Employees)
            .select(Employees.name, min(Employees.salary).over())
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:50", "marry:50", "tom:50", "penny:50"), results.toSet())
    }

    @Test
    fun testRunningMin() {
        val results = database
            .from(Employees)
            .select(Employees.name, min(Employees.salary).over { orderBy(Employees.id.asc()) })
            .map { row ->
                "${row.getString(1)}:${row.getLong(2)}"
            }

        assertEquals(setOf("vince:100", "marry:50", "tom:50", "penny:50"), results.toSet())
    }
}