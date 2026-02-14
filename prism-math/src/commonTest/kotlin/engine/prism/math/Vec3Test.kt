package engine.prism.math

import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class Vec3Test {

    private val epsilon = 1e-5f

    private fun Float.shouldBeApprox(expected: Float) {
        assertTrue(
            MathUtils.approximately(this, expected, epsilon),
            "Expected $expected but got $this (epsilon=$epsilon)"
        )
    }

    private fun Vec3.shouldBeApprox(expected: Vec3) {
        x.shouldBeApprox(expected.x)
        y.shouldBeApprox(expected.y)
        z.shouldBeApprox(expected.z)
    }

    // --- Normalize ---

    @Test
    fun normalizeUnitVector() {
        val v = Vec3(1f, 0f, 0f)
        v.normalize().shouldBeApprox(Vec3(1f, 0f, 0f))
    }

    @Test
    fun normalizeArbitraryVector() {
        val v = Vec3(3f, 4f, 0f)
        val n = v.normalize()
        n.length().shouldBeApprox(1f)
        n.shouldBeApprox(Vec3(0.6f, 0.8f, 0f))
    }

    @Test
    fun normalizeZeroVectorReturnsZero() {
        val v = Vec3(0f, 0f, 0f)
        v.normalize() shouldBe Vec3.ZERO
    }

    // --- Cross product ---

    @Test
    fun crossProductOfBasisVectors() {
        val i = Vec3(1f, 0f, 0f)
        val j = Vec3(0f, 1f, 0f)
        val k = Vec3(0f, 0f, 1f)

        i.cross(j).shouldBeApprox(k)
        j.cross(k).shouldBeApprox(i)
        k.cross(i).shouldBeApprox(j)
    }

    @Test
    fun crossProductAnticommutative() {
        val a = Vec3(1f, 2f, 3f)
        val b = Vec3(4f, 5f, 6f)

        a.cross(b).shouldBeApprox(-b.cross(a))
    }

    @Test
    fun crossProductOfParallelVectorsIsZero() {
        val a = Vec3(2f, 4f, 6f)
        val b = Vec3(1f, 2f, 3f)

        a.cross(b).shouldBeApprox(Vec3.ZERO)
    }

    // --- Dot product ---

    @Test
    fun dotProductOfOrthogonalVectorsIsZero() {
        val a = Vec3(1f, 0f, 0f)
        val b = Vec3(0f, 1f, 0f)

        a.dot(b).shouldBeApprox(0f)
    }

    @Test
    fun dotProductOfParallelVectors() {
        val a = Vec3(2f, 0f, 0f)
        val b = Vec3(3f, 0f, 0f)

        a.dot(b).shouldBeApprox(6f)
    }

    @Test
    fun dotProductGeneral() {
        val a = Vec3(1f, 2f, 3f)
        val b = Vec3(4f, 5f, 6f)

        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        a.dot(b).shouldBeApprox(32f)
    }

    // --- Length ---

    @Test
    fun lengthOfUnitVector() {
        Vec3(1f, 0f, 0f).length().shouldBeApprox(1f)
    }

    @Test
    fun lengthOfArbitraryVector() {
        Vec3(3f, 4f, 0f).length().shouldBeApprox(5f)
    }

    @Test
    fun lengthSquared() {
        Vec3(1f, 2f, 3f).lengthSquared().shouldBeApprox(14f)
    }

    @Test
    fun lengthOf3dVector() {
        Vec3(1f, 2f, 2f).length().shouldBeApprox(3f)
    }

    // --- Operator overloads ---

    @Test
    fun addition() {
        val a = Vec3(1f, 2f, 3f)
        val b = Vec3(4f, 5f, 6f)

        (a + b).shouldBeApprox(Vec3(5f, 7f, 9f))
    }

    @Test
    fun subtraction() {
        val a = Vec3(4f, 5f, 6f)
        val b = Vec3(1f, 2f, 3f)

        (a - b).shouldBeApprox(Vec3(3f, 3f, 3f))
    }

    @Test
    fun scalarMultiplication() {
        val v = Vec3(1f, 2f, 3f)

        (v * 2f).shouldBeApprox(Vec3(2f, 4f, 6f))
    }

    @Test
    fun scalarMultiplicationCommutative() {
        val v = Vec3(1f, 2f, 3f)

        (2f * v).shouldBeApprox(Vec3(2f, 4f, 6f))
    }

    @Test
    fun componentWiseMultiplication() {
        val a = Vec3(2f, 3f, 4f)
        val b = Vec3(5f, 6f, 7f)

        (a * b).shouldBeApprox(Vec3(10f, 18f, 28f))
    }

    @Test
    fun scalarDivision() {
        val v = Vec3(2f, 4f, 6f)

        (v / 2f).shouldBeApprox(Vec3(1f, 2f, 3f))
    }

    @Test
    fun unaryMinus() {
        val v = Vec3(1f, -2f, 3f)

        (-v).shouldBeApprox(Vec3(-1f, 2f, -3f))
    }

    // --- Distance ---

    @Test
    fun distanceToSelf() {
        val v = Vec3(3f, 4f, 5f)
        v.distanceTo(v).shouldBeApprox(0f)
    }

    @Test
    fun distanceToOther() {
        val a = Vec3(1f, 0f, 0f)
        val b = Vec3(4f, 0f, 0f)

        a.distanceTo(b).shouldBeApprox(3f)
    }

    // --- Lerp ---

    @Test
    fun lerpAtZero() {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 20f, 30f)

        a.lerp(b, 0f).shouldBeApprox(a)
    }

    @Test
    fun lerpAtOne() {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 20f, 30f)

        a.lerp(b, 1f).shouldBeApprox(b)
    }

    @Test
    fun lerpAtHalf() {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 20f, 30f)

        a.lerp(b, 0.5f).shouldBeApprox(Vec3(5f, 10f, 15f))
    }

    // --- Companion constants ---

    @Test
    fun companionConstants() {
        Vec3.ZERO shouldBe Vec3(0f, 0f, 0f)
        Vec3.ONE shouldBe Vec3(1f, 1f, 1f)
        Vec3.UP shouldBe Vec3(0f, 1f, 0f)
        Vec3.DOWN shouldBe Vec3(0f, -1f, 0f)
        Vec3.RIGHT shouldBe Vec3(1f, 0f, 0f)
        Vec3.LEFT shouldBe Vec3(-1f, 0f, 0f)
        Vec3.FORWARD shouldBe Vec3(0f, 0f, -1f)
        Vec3.BACK shouldBe Vec3(0f, 0f, 1f)
    }
}
