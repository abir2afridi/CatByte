package com.example.engine

/**
 * A GLSL ES 3.0 effect. [body] runs after the source colour is sampled, so an
 * effect only describes what it changes; the wrapper owns the boilerplate.
 */
data class GlslEffect(
    val id: String,
    val displayName: String,
    val category: String,
    val intensity: Float = 0.6f,
    val params: Map<String, Float> = emptyMap(),
    val body: String
) {
    val fragmentSource: String get() = ShaderTemplate.wrap(body)
}

object ShaderTemplate {
    const val HEADER = "#version 300 es\nprecision mediump float;"

    fun wrap(body: String): String = """
$HEADER
uniform sampler2D uTex;
uniform vec2 uTexel;
uniform float uTime;
uniform float uIntensity;
uniform vec4 uParams;
in vec2 vUv;
out vec4 fragColor;

void main() {
  vec2 uv = vUv;
  vec4 src = texture(uTex, uv);
  vec4 outColor = src;
  $body
  fragColor = mix(src, outColor, clamp(uIntensity, 0.0, 1.0));
}
"""
}

/**
 * Shader library for the compositor. Every effect is a single fragment pass over
 * the clip texture, which is what keeps 30+ effects affordable on a phone.
 */
object EffectShaders {

    private fun effect(
        id: String,
        name: String,
        category: String,
        body: String,
        intensity: Float = 0.6f,
        params: Map<String, Float> = emptyMap()
    ) = GlslEffect(id, name, category, intensity, params, body.trimIndent())

    val ALL: List<GlslEffect> = listOf(
        effect(
            "rgb_split", "RGB Split", "glitch",
            """
            float amt = uParams.x * 0.01 * (0.5 + 0.5 * sin(uTime));
            outColor.r = texture(uTex, uv + vec2(amt, 0.0)).r;
            outColor.b = texture(uTex, uv - vec2(amt, 0.0)).b;
            """,
            params = mapOf("amount" to 4f)
        ),
        effect(
            "shake", "Beat Shake", "motion",
            """
            vec2 off = vec2(sin(uTime * 42.0), cos(uTime * 37.0)) * uParams.x * 0.002;
            outColor = texture(uTex, uv + off);
            """,
            params = mapOf("amount" to 6f)
        ),
        effect(
            "vhs_grain", "VHS Grain", "retro",
            """
            float line = sin(uv.y * 900.0) * 0.02;
            vec3 c = texture(uTex, uv + vec2(line, 0.0)).rgb;
            float noise = fract(sin(dot(uv * uTime, vec2(12.9898, 78.233))) * 43758.5453);
            outColor = vec4(c + (noise - 0.5) * 0.12, src.a);
            """,
            params = mapOf("grain" to 0.12f)
        ),
        effect(
            "neon_halo", "Neon Halo", "glow",
            """
            vec3 glow = vec3(0.0);
            for (int i = 0; i < 8; i++) {
              float a = float(i) * 0.785;
              glow += texture(uTex, uv + vec2(cos(a), sin(a)) * uParams.x * 0.004).rgb;
            }
            outColor = vec4(src.rgb + glow / 8.0 * 0.9, src.a);
            """,
            params = mapOf("radius" to 5f)
        ),
        effect(
            "lightning", "Lightning", "stylize",
            """
            float bolt = step(0.97, fract(sin(floor(uv.x * 60.0) + uTime * 3.0) * 43758.5453));
            outColor = vec4(src.rgb + bolt * 0.7, src.a);
            """
        ),
        effect(
            "zoom_blur", "Zoom Blur", "motion",
            """
            vec2 dir = uv - vec2(0.5);
            vec4 acc = vec4(0.0);
            for (int i = 0; i < 8; i++) {
              float t = float(i) / 7.0;
              acc += texture(uTex, uv - dir * t * uParams.x * 0.02);
            }
            outColor = acc / 8.0;
            """,
            params = mapOf("amount" to 8f)
        ),
        effect(
            "glitch_blocks", "Glitch Blocks", "glitch",
            """
            float block = floor(uv.y * 24.0);
            float seed = fract(sin(block * 91.7 + floor(uTime * 8.0)) * 43758.5453);
            float shift = (seed - 0.5) * 0.12 * step(0.7, seed);
            outColor = texture(uTex, uv + vec2(shift, 0.0));
            """
        ),
        effect(
            "scanlines", "Scanlines", "retro",
            """
            float line = 0.85 + 0.15 * sin(uv.y * 1200.0);
            outColor = vec4(src.rgb * line, src.a);
            """
        ),
        effect(
            "film_grain", "Film Grain", "retro",
            """
            float g = fract(sin(dot(uv + uTime, vec2(12.9898, 78.233))) * 43758.5453);
            outColor = vec4(src.rgb + (g - 0.5) * 0.12, src.a);
            """,
            params = mapOf("grain" to 0.12f)
        ),
        effect(
            "vignette_soft", "Vignette", "stylize",
            """
            float d = distance(uv, vec2(0.5));
            float v = smoothstep(uParams.x * 0.02, 0.9, d);
            outColor = vec4(src.rgb * (1.0 - v * 0.85), src.a);
            """,
            params = mapOf("strength" to 40f)
        ),
        effect(
            "bloom", "Bloom", "glow",
            """
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 6; i++) {
              float a = float(i) * 1.047;
              sum += texture(uTex, uv + vec2(cos(a), sin(a)) * uParams.x * 0.006).rgb;
            }
            vec3 bright = max(sum / 6.0 - 0.55, 0.0);
            outColor = vec4(src.rgb + bright * 1.6, src.a);
            """,
            params = mapOf("radius" to 6f)
        ),
        effect(
            "sharpen", "Sharpen", "detail",
            """
            vec3 n = texture(uTex, uv + vec2(0.0, -uTexel.y)).rgb
                   + texture(uTex, uv + vec2(-uTexel.x, 0.0)).rgb
                   + texture(uTex, uv + vec2(uTexel.x, 0.0)).rgb
                   + texture(uTex, uv + vec2(0.0, uTexel.y)).rgb;
            outColor = vec4(src.rgb + (src.rgb * 4.0 - n) * uParams.x * 0.05, src.a);
            """,
            params = mapOf("amount" to 6f)
        ),
        effect(
            "soften_blur", "Soft Focus", "detail",
            """
            vec3 acc = vec3(0.0);
            for (int i = 0; i < 9; i++) {
              float x = float(i % 3) - 1.0;
              float y = float(i / 3) - 1.0;
              acc += texture(uTex, uv + vec2(x, y) * uTexel * uParams.x).rgb;
            }
            outColor = vec4(acc / 9.0, src.a);
            """,
            params = mapOf("radius" to 3f)
        ),
        effect(
            "pixelate", "Pixelate", "stylize",
            """
            float px = uParams.x * 0.002 + 0.001;
            vec2 q = floor(uv / px) * px;
            outColor = texture(uTex, q);
            """,
            params = mapOf("size" to 12f)
        ),
        effect(
            "posterize", "Posterize", "stylize",
            """
            float levels = max(2.0, uParams.x);
            outColor = vec4(floor(src.rgb * levels + 0.5) / levels, src.a);
            """,
            params = mapOf("levels" to 6f)
        ),
        effect(
            "duotone", "Duotone", "color",
            """
            float l = dot(src.rgb, vec3(0.299, 0.587, 0.114));
            vec3 shadow = vec3(0.05, 0.08, 0.25);
            vec3 light = vec3(1.0, 0.75, 0.35);
            outColor = vec4(mix(shadow, light, l), src.a);
            """
        ),
        effect(
            "mirror_kaleido", "Kaleidoscope", "stylize",
            """
            vec2 p = abs(uv - 0.5);
            outColor = texture(uTex, vec2(0.5 + p.x, uv.y));
            """
        ),
        effect(
            "chroma_wave", "Chroma Wave", "glitch",
            """
            vec2 w = vec2(sin(uv.y * 18.0 + uTime * 3.0) * uParams.x * 0.0015, 0.0);
            outColor.r = texture(uTex, uv + w).r;
            outColor.g = src.g;
            outColor.b = texture(uTex, uv - w).b;
            """,
            params = mapOf("amount" to 10f)
        ),
        effect(
            "edge_detect", "Edge Detect", "detail",
            """
            vec3 gx = texture(uTex, uv + vec2(uTexel.x, 0.0)).rgb - texture(uTex, uv - vec2(uTexel.x, 0.0)).rgb;
            vec3 gy = texture(uTex, uv + vec2(0.0, uTexel.y)).rgb - texture(uTex, uv - vec2(0.0, uTexel.y)).rgb;
            float e = length(gx) + length(gy);
            outColor = vec4(vec3(e), src.a);
            """
        ),
        effect(
            "emboss", "Emboss", "detail",
            """
            vec3 d = texture(uTex, uv + uTexel).rgb - texture(uTex, uv - uTexel).rgb;
            outColor = vec4(vec3(0.5) + d * 0.8, src.a);
            """
        ),
        effect(
            "halftone", "Halftone", "stylize",
            """
            float cell = uParams.x * 0.001 + 0.002;
            vec2 q = fract(uv / cell) - 0.5;
            float l = dot(src.rgb, vec3(0.299, 0.587, 0.114));
            float dotMask = step(length(q), l * 0.7);
            outColor = vec4(vec3(dotMask), src.a);
            """,
            params = mapOf("size" to 8f)
        ),
        effect(
            "sketch_pencil", "Pencil Sketch", "stylize",
            """
            float l = dot(src.rgb, vec3(0.299, 0.587, 0.114));
            float edge = 1.0 - smoothstep(0.02, 0.18, abs(l - dot(texture(uTex, uv + uTexel).rgb, vec3(0.299, 0.587, 0.114))));
            outColor = vec4(vec3(edge), src.a);
            """
        ),
        effect(
            "ink_bleed", "Ink Bleed", "stylize",
            """
            vec3 acc = vec3(0.0);
            for (int i = 0; i < 5; i++) {
              float r = float(i) * 0.004 * uParams.x;
              acc += texture(uTex, uv + vec2(r, r * 0.6)).rgb;
            }
            outColor = vec4(mix(src.rgb, acc / 5.0, 0.6), src.a);
            """,
            params = mapOf("spread" to 5f)
        ),
        effect(
            "paper_grain", "Paper Grain", "retro",
            """
            float n = fract(sin(dot(floor(uv * 640.0), vec2(12.9898, 78.233))) * 43758.5453);
            outColor = vec4(src.rgb * (0.94 + n * 0.12), src.a);
            """
        ),
        effect(
            "dreamy_glow", "Dreamy Glow", "glow",
            """
            vec3 blur = texture(uTex, uv + vec2(uTexel.x, 0.0)).rgb + texture(uTex, uv - vec2(uTexel.x, 0.0)).rgb;
            outColor = vec4(mix(src.rgb, (blur * 0.5 + src.rgb) * 1.08, 0.55), src.a);
            """
        ),
        effect(
            "retro_80s", "Retro 80s", "retro",
            """
            float l = dot(src.rgb, vec3(0.299, 0.587, 0.114));
            vec3 tint = mix(vec3(0.35, 0.05, 0.55), vec3(0.1, 0.9, 0.95), l);
            outColor = vec4(mix(src.rgb, tint, 0.55), src.a);
            """
        ),
        effect(
            "hologram", "Hologram", "stylize",
            """
            float l = dot(src.rgb, vec3(0.299, 0.587, 0.114));
            vec3 holo = 0.5 + 0.5 * cos(6.28318 * (l + vec3(0.0, 0.33, 0.67) + uTime * 0.05));
            outColor = vec4(mix(src.rgb, holo, 0.65), src.a);
            """
        ),
        effect(
            "thermal", "Thermal", "color",
            """
            float l = dot(src.rgb, vec3(0.299, 0.587, 0.114));
            vec3 cold = vec3(0.05, 0.15, 0.65);
            vec3 hot = vec3(1.0, 0.25, 0.0);
            outColor = vec4(mix(cold, hot, smoothstep(0.15, 0.85, l)), src.a);
            """
        ),
        effect(
            "xray", "X-Ray", "color",
            """
            float l = 1.0 - dot(src.rgb, vec3(0.299, 0.587, 0.114));
            outColor = vec4(vec3(0.6, 0.85, 1.0) * l, src.a);
            """
        ),
        effect(
            "cinematic_lut", "Cinematic Grade", "color",
            """
            vec3 c = pow(src.rgb, vec3(1.05));
            c = mix(c, c * vec3(1.06, 1.0, 0.94), 0.7);
            float l = dot(c, vec3(0.299, 0.587, 0.114));
            c = mix(vec3(l), c, 1.08);
            outColor = vec4(c, src.a);
            """
        ),
        effect(
            "lens_flare", "Lens Flare", "glow",
            """
            float d = distance(uv, vec2(0.78, 0.22));
            float flare = smoothstep(0.35, 0.0, d) * (1.4 + sin(uTime) * 0.1);
            outColor = vec4(src.rgb + vec3(1.0, 0.85, 0.6) * flare, src.a);
            """
        ),
        effect(
            "wave_warp", "Wave Warp", "motion",
            """
            vec2 w = vec2(sin(uv.y * 12.0 + uTime * 2.0), 0.0) * uParams.x * 0.002;
            outColor = texture(uTex, uv + w);
            """,
            params = mapOf("amplitude" to 10f)
        ),
        effect(
            "bulge", "Bulge", "motion",
            """
            vec2 p = uv - 0.5;
            float r = length(p);
            float k = 1.0 - smoothstep(0.0, 0.5, r);
            outColor = texture(uTex, 0.5 + p * (1.0 - k * uParams.x * 0.02));
            """,
            params = mapOf("amount" to 6f)
        ),
        effect(
            "starburst", "Starburst", "glow",
            """
            vec2 p = uv - 0.5;
            float a = atan(p.y, p.x);
            float rays = pow(abs(sin(a * 9.0)), 8.0) * smoothstep(0.6, 0.0, length(p));
            outColor = vec4(src.rgb + rays * 0.7, src.a);
            """
        ),
        effect(
            "noise_dissolve", "Noise Dissolve", "glitch",
            """
            float n = fract(sin(dot(floor(uv * 320.0), vec2(12.9898, 78.233))) * 43758.5453);
            float mask = step(uIntensity, n);
            outColor = vec4(src.rgb * mask, src.a);
            """
        )
    )

    private val byId = ALL.associateBy { it.id }

    fun find(id: String): GlslEffect? = byId[id]

    fun byCategory(category: String): List<GlslEffect> = ALL.filter { it.category == category }

    fun categories(): List<String> = ALL.map { it.category }.distinct().sorted()

    /** Effects wired to the editor's legacy enum, so old projects keep working. */
    fun forLegacyEffect(typeName: String): GlslEffect? = when (typeName) {
        "RGB_SPLIT" -> find("rgb_split")
        "SHAKE" -> find("shake")
        "VHS_GRAIN" -> find("vhs_grain")
        "NEON_HALO" -> find("neon_halo")
        "LIGHTNING" -> find("lightning")
        "ZOOM_BLUR" -> find("zoom_blur")
        else -> null
    }

    /** Hairline mask helper used by the mask sheet preview. */
    fun maskPreviewBody(kind: String, feather: Float): String = when (kind) {
        "CIRCLE" -> "float d = distance(uv, vec2(0.5));\n  outColor.a *= 1.0 - smoothstep(0.5 - $feather, 0.5, d);"
        "RECTANGLE" -> "vec2 d = abs(uv - 0.5);\n  outColor.a *= 1.0 - smoothstep(0.45 - $feather, 0.5, max(d.x, d.y));"
        "SPLIT" -> "outColor.a *= step(0.5, uv.x);"
        "MIRROR" -> "outColor.a *= step(0.5, 1.0 - uv.x);"
        else -> ""
    }
}
