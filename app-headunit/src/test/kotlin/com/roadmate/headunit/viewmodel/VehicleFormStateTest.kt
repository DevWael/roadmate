package com.roadmate.headunit.viewmodel

import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("VehicleFormState")
class VehicleFormStateTest {

    private lateinit var validForm: VehicleFormState

    @BeforeEach
    fun setUp() {
        validForm = VehicleFormState(
            name = "My Car",
            make = "Mitsubishi",
            model = "Lancer EX",
            year = "2015",
            engineType = EngineType.INLINE_4,
            engineSize = "1.6",
            fuelType = FuelType.GASOLINE,
            plateNumber = "ABC-123",
            odometerKm = "85000",
            odometerUnit = OdometerUnit.KM,
            cityConsumption = "9.5",
            highwayConsumption = "6.8",
        )
    }

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        fun `valid form has no errors`() {
            val result = validForm.validate()
            assertTrue(result.errors.isEmpty())
        }

        @Test
        fun `isValid returns true for valid form`() {
            assertTrue(validForm.isValid())
        }

        @Test
        fun `blank name produces error`() {
            val form = validForm.copy(name = "")
            val result = form.validate()
            assertTrue(result.errors.containsKey("name"))
        }

        @Test
        fun `blank name error message is Required`() {
            val form = validForm.copy(name = "")
            val result = form.validate()
            assertEquals("Required", result.errors["name"])
        }

        @Test
        fun `whitespace-only name produces error`() {
            val form = validForm.copy(name = "   ")
            val result = form.validate()
            assertTrue(result.errors.containsKey("name"))
        }

        @Test
        fun `blank make produces error`() {
            val form = validForm.copy(make = "")
            assertTrue(form.validate().errors.containsKey("make"))
        }

        @Test
        fun `blank model produces error`() {
            val form = validForm.copy(model = "")
            assertTrue(form.validate().errors.containsKey("model"))
        }

        @Test
        fun `blank year produces error`() {
            val form = validForm.copy(year = "")
            assertTrue(form.validate().errors.containsKey("year"))
        }

        @Test
        fun `non-numeric year produces error`() {
            val form = validForm.copy(year = "abc")
            val result = form.validate()
            assertEquals("Invalid year", result.errors["year"])
        }

        @Test
        fun `year below 1900 produces error`() {
            val form = validForm.copy(year = "1899")
            assertEquals("Invalid year", form.validate().errors["year"])
        }

        @Test
        fun `year above 2100 produces error`() {
            val form = validForm.copy(year = "2101")
            assertEquals("Invalid year", form.validate().errors["year"])
        }

        @Test
        fun `year 1900 is valid`() {
            val form = validForm.copy(year = "1900")
            assertFalse(form.validate().errors.containsKey("year"))
        }

        @Test
        fun `year 2100 is valid`() {
            val form = validForm.copy(year = "2100")
            assertFalse(form.validate().errors.containsKey("year"))
        }

        @Test
        fun `blank engine size produces error`() {
            val form = validForm.copy(engineSize = "")
            assertTrue(form.validate().errors.containsKey("engineSize"))
        }

        @Test
        fun `zero engine size produces error`() {
            val form = validForm.copy(engineSize = "0")
            assertEquals("Must be positive", form.validate().errors["engineSize"])
        }

        @Test
        fun `negative engine size produces error`() {
            val form = validForm.copy(engineSize = "-1.5")
            assertTrue(form.validate().errors.containsKey("engineSize"))
        }

        @Test
        fun `blank plate number produces error`() {
            val form = validForm.copy(plateNumber = "")
            assertTrue(form.validate().errors.containsKey("plateNumber"))
        }

        @Test
        fun `blank odometer produces error`() {
            val form = validForm.copy(odometerKm = "")
            assertTrue(form.validate().errors.containsKey("odometerKm"))
        }

        @Test
        fun `negative odometer produces error`() {
            val form = validForm.copy(odometerKm = "-100")
            assertEquals("Invalid", form.validate().errors["odometerKm"])
        }

        @Test
        fun `zero odometer is valid`() {
            val form = validForm.copy(odometerKm = "0")
            assertFalse(form.validate().errors.containsKey("odometerKm"))
        }

        @Test
        fun `non-numeric odometer produces error`() {
            val form = validForm.copy(odometerKm = "abc")
            assertEquals("Invalid", form.validate().errors["odometerKm"])
        }

        @Test
        fun `blank city consumption produces error`() {
            val form = validForm.copy(cityConsumption = "")
            assertTrue(form.validate().errors.containsKey("cityConsumption"))
        }

        @Test
        fun `zero city consumption produces error`() {
            val form = validForm.copy(cityConsumption = "0")
            assertEquals("Must be positive", form.validate().errors["cityConsumption"])
        }

        @Test
        fun `blank highway consumption produces error`() {
            val form = validForm.copy(highwayConsumption = "")
            assertTrue(form.validate().errors.containsKey("highwayConsumption"))
        }

        @Test
        fun `zero highway consumption produces error`() {
            val form = validForm.copy(highwayConsumption = "0")
            assertEquals("Must be positive", form.validate().errors["highwayConsumption"])
        }

        @Test
        fun `multiple errors reported simultaneously`() {
            val form = VehicleFormState()
            val result = form.validate()
            assertTrue(result.errors.size >= 8)
            assertTrue(result.errors.containsKey("name"))
            assertTrue(result.errors.containsKey("make"))
            assertTrue(result.errors.containsKey("model"))
            assertTrue(result.errors.containsKey("year"))
            assertTrue(result.errors.containsKey("engineSize"))
            assertTrue(result.errors.containsKey("plateNumber"))
            assertTrue(result.errors.containsKey("cityConsumption"))
            assertTrue(result.errors.containsKey("highwayConsumption"))
        }
    }

    @Nested
    @DisplayName("toVehicle conversion")
    inner class ToVehicleConversion {

        @Test
        fun `valid form converts to Vehicle`() {
            val vehicle = validForm.toVehicle()
            assertNotNull(vehicle)
            assertEquals("My Car", vehicle!!.name)
            assertEquals("Mitsubishi", vehicle.make)
            assertEquals("Lancer EX", vehicle.model)
            assertEquals(2015, vehicle.year)
            assertEquals(EngineType.INLINE_4, vehicle.engineType)
            assertEquals(1.6, vehicle.engineSize, 0.001)
            assertEquals(FuelType.GASOLINE, vehicle.fuelType)
            assertEquals("ABC-123", vehicle.plateNumber)
            assertEquals(85000.0, vehicle.odometerKm, 0.001)
            assertEquals(OdometerUnit.KM, vehicle.odometerUnit)
            assertEquals(9.5, vehicle.cityConsumption, 0.001)
            assertEquals(6.8, vehicle.highwayConsumption, 0.001)
        }

        @Test
        fun `invalid form returns null`() {
            val form = VehicleFormState()
            assertNull(form.toVehicle())
        }

        @Test
        fun `toVehicle trims whitespace from text fields`() {
            val form = validForm.copy(
                name = "  My Car  ",
                make = "  Mitsubishi  ",
                model = "  Lancer EX  ",
                plateNumber = "  ABC-123  ",
            )
            val vehicle = form.toVehicle()
            assertNotNull(vehicle)
            assertEquals("My Car", vehicle!!.name)
            assertEquals("Mitsubishi", vehicle.make)
            assertEquals("Lancer EX", vehicle.model)
            assertEquals("ABC-123", vehicle.plateNumber)
        }

        @Test
        fun `toVehicle generates unique IDs`() {
            val v1 = validForm.toVehicle()
            val v2 = validForm.toVehicle()
            assertNotNull(v1)
            assertNotNull(v2)
            assertTrue(v1!!.id != v2!!.id)
        }

        @Test
        fun `toVehicle preserves odometer unit`() {
            val milesForm = validForm.copy(odometerUnit = OdometerUnit.MILES)
            val vehicle = milesForm.toVehicle()
            assertNotNull(vehicle)
            assertEquals(OdometerUnit.MILES, vehicle!!.odometerUnit)
            assertEquals(85000.0, vehicle.odometerKm, 0.001)
        }
    }

    @Nested
    @DisplayName("defaults")
    inner class Defaults {

        @Test
        fun `default form has empty strings`() {
            val form = VehicleFormState()
            assertEquals("", form.name)
            assertEquals("", form.make)
            assertEquals("", form.model)
            assertEquals("", form.year)
            assertEquals("", form.engineSize)
            assertEquals("", form.plateNumber)
            assertEquals("", form.odometerKm)
            assertEquals("", form.cityConsumption)
            assertEquals("", form.highwayConsumption)
        }

        @Test
        fun `default form has INLINE_4 engine type`() {
            assertEquals(EngineType.INLINE_4, VehicleFormState().engineType)
        }

        @Test
        fun `default form has GASOLINE fuel type`() {
            assertEquals(FuelType.GASOLINE, VehicleFormState().fuelType)
        }

        @Test
        fun `default form has KM odometer unit`() {
            assertEquals(OdometerUnit.KM, VehicleFormState().odometerUnit)
        }

        @Test
        fun `default form has no errors`() {
            assertTrue(VehicleFormState().errors.isEmpty())
        }

        @Test
        fun `default template selection is NONE`() {
            assertEquals(TemplateSelection.NONE, VehicleFormState().templateSelection)
        }
    }
}
