package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SmartAvatarBg
import com.example.ui.theme.SmartBorder
import com.example.ui.theme.SmartInkMuted
import com.example.ui.theme.SmartSlateTitle

@Composable
fun SmartSectionHeaderWithLine(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SmartSlateTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = SmartBorder
        )
    }
}

@Composable
fun SmartProfileAvatarButton(
    onClick: () -> Unit,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = SmartAvatarBg,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Open navigation menu",
                tint = SmartSlateTitle,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}

/**
 * Pure Kotlin ISO/IEC 18004 Version 3-L (29x29) Byte-mode QR Code Generator.
 * Supports up to 53 ASCII bytes (e.g. "http://192.168.1.105:8765").
 */
object QrCodeEncoder {
    private const val SIZE = 29
    private const val DATA_CODEWORDS = 55
    private const val EC_CODEWORDS = 15
    private const val FORMAT_L_MASK0 = 0b111011111000100

    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfLog[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            gfExp[i] = gfExp[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }

    private fun generatorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val factor = gfExp[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
            }
            poly = next
        }
        return poly
    }

    private fun computeEcc(data: IntArray, ecLen: Int): IntArray {
        val gen = generatorPoly(ecLen)
        val res = IntArray(data.size + ecLen)
        data.copyInto(res)
        for (i in data.indices) {
            val coef = res[i]
            if (coef != 0) {
                for (j in gen.indices) {
                    res[i + j] = res[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        return res.copyOfRange(data.size, res.size)
    }

    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8).take(53)
        val bits = ArrayList<Int>(DATA_CODEWORDS * 8)

        fun pushBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Byte mode indicator (0100) + 8-bit length
        pushBits(0b0100, 4)
        pushBits(bytes.size, 8)
        for (b in bytes) {
            pushBits(b.toInt() and 0xFF, 8)
        }
        // Terminator up to 4 bits
        val maxBits = DATA_CODEWORDS * 8
        val term = minOf(4, maxBits - bits.size)
        pushBits(0, term)
        while (bits.size % 8 != 0) bits.add(0)

        val dataCw = IntArray(DATA_CODEWORDS)
        var cwIdx = 0
        var i = 0
        while (i < bits.size && cwIdx < DATA_CODEWORDS) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i + b]
            }
            dataCw[cwIdx++] = v
            i += 8
        }
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (cwIdx < DATA_CODEWORDS) {
            dataCw[cwIdx++] = padBytes[padIdx++ % 2]
        }

        val ecc = computeEcc(dataCw, EC_CODEWORDS)
        val allCodewords = IntArray(DATA_CODEWORDS + EC_CODEWORDS)
        dataCw.copyInto(allCodewords)
        ecc.copyInto(allCodewords, DATA_CODEWORDS)

        val modules = Array(SIZE) { BooleanArray(SIZE) }
        val isFunction = Array(SIZE) { BooleanArray(SIZE) }

        fun setFunc(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until SIZE && c in 0 until SIZE) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        fun drawFinder(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r !in 0 until SIZE || c !in 0 until SIZE) continue
                    val dark = dr in 0..6 && dc in 0..6 && (
                        dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4)
                    )
                    setFunc(r, c, dark)
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, SIZE - 7)
        drawFinder(SIZE - 7, 0)

        // Alignment pattern at (22, 22) for Version 3
        for (dr in -2..2) {
            for (dc in -2..2) {
                val dark = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc)) != 1
                setFunc(22 + dr, 22 + dc, dark)
            }
        }

        // Timing patterns
        for (idx in 8 until SIZE - 8) {
            val dark = (idx % 2 == 0)
            if (!isFunction[6][idx]) setFunc(6, idx, dark)
            if (!isFunction[idx][6]) setFunc(idx, 6, dark)
        }

        // Dark module
        setFunc(SIZE - 8, 8, true)

        // Reserve & write format info (EC-L, Mask 0)
        for (k in 0..5) setFunc(8, k, ((FORMAT_L_MASK0 ushr (14 - k)) and 1) == 1)
        setFunc(8, 7, ((FORMAT_L_MASK0 ushr 8) and 1) == 1)
        setFunc(8, 8, ((FORMAT_L_MASK0 ushr 7) and 1) == 1)
        setFunc(7, 8, ((FORMAT_L_MASK0 ushr 6) and 1) == 1)
        for (k in 0..5) setFunc(5 - k, 8, ((FORMAT_L_MASK0 ushr (5 - k)) and 1) == 1)

        for (k in 0..6) setFunc(SIZE - 1 - k, 8, ((FORMAT_L_MASK0 ushr (14 - k)) and 1) == 1)
        for (k in 0..7) setFunc(8, SIZE - 8 + k, ((FORMAT_L_MASK0 ushr (7 - k)) and 1) == 1)

        // Place data + ECC bits with Mask 0: (r + c) % 2 == 0
        var bitIndex = 0
        val totalBits = allCodewords.size * 8
        var col = SIZE - 1
        var upward = true
        while (col > 0) {
            if (col == 6) col--
            for (step in 0 until SIZE) {
                val r = if (upward) SIZE - 1 - step else step
                for (dx in 0..1) {
                    val c = col - dx
                    if (!isFunction[r][c]) {
                        val bit = if (bitIndex < totalBits) {
                            (allCodewords[bitIndex ushr 3] ushr (7 - (bitIndex and 7))) and 1
                        } else 0
                        bitIndex++
                        val masked = if ((r + c) % 2 == 0) bit xor 1 else bit
                        modules[r][c] = masked == 1
                    }
                }
            }
            col -= 2
            upward = !upward
        }

        return modules
    }
}

@Composable
fun ScannableQrCodeCard(
    content: String,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(content) { QrCodeEncoder.encode(content) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SmartBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(PureWhite)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val n = matrix.size
                val quiet = 2
                val cell = this.size.width / (n + quiet * 2)
                drawRect(color = PureWhite, size = this.size)
                for (r in 0 until n) {
                    for (c in 0 until n) {
                        if (matrix[r][c]) {
                            drawRect(
                                color = PureBlack,
                                topLeft = Offset((c + quiet) * cell, (r + quiet) * cell),
                                size = Size(cell + 0.5f, cell + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
