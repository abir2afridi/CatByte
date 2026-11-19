package com.example.engine.filters

import com.example.model.VideoFilter
import com.example.engine.filters.hit.*

/**
 * The Filter drawer's tab list. Membership lives in each filter's own file,
 * so adding a filter to a tab never touches this catalog or another filter.
 */
object FilterCatalog {

    /** Shared cover photo used by every card in the Hit tab. */
    const val HIT_COVER_PATH = "file:///android_asset/cover/hit.jpg"


    /** The drawer tabs (label shown in the sheet). */
    val tabs: List<String> = listOf(
        "Trending", "Hit", "Pencil", "Cinematic", "Vintage", "Neon", "B&W", "Cool", "Warm", "All", "My LUTs"
    )

    /** Every non-original filter with its grade (and card/tab metadata). */
    val all: List<Pair<VideoFilter, FilterGrade>> = listOf(
        VideoFilter.SKETCH_GRAPHITE to SketchGraphiteFilter.grade,
        VideoFilter.BLUEPRINT to BlueprintFilter.grade,
        VideoFilter.SEPIA_NOTEBOOK to SepiaNotebookFilter.grade,
        VideoFilter.CHARCOAL_SMUDGE to CharcoalSmudgeFilter.grade,
        VideoFilter.HALFTONE_MANGA to HalftoneMangaFilter.grade,
        VideoFilter.CYBERPUNK to CyberpunkFilter.grade,
        VideoFilter.VINTAGE to VintageFilter.grade,
        VideoFilter.GOLDEN_HOUR to GoldenHourFilter.grade,
        VideoFilter.NOIR to NoirFilter.grade,
        VideoFilter.TEAL_ORANGE to TealOrangeFilter.grade,
        VideoFilter.MOODY to MoodyFilter.grade,
        VideoFilter.GLITCH_ART to GlitchArtFilter.grade,
        VideoFilter.TROPICAL_HUES to TropicalHuesFilter.grade,
        VideoFilter.RETRO_PRINT to RetroPrintFilter.grade,
        VideoFilter.CLEAR_SKY to ClearSkyFilter.grade,
        VideoFilter.LIGHT_PASTEL to LightPastelFilter.grade,
        VideoFilter.GOLDEN_SUNSET to GoldenSunsetFilter.grade,
        VideoFilter.NEON_FIRE to NeonFireFilter.grade,
        VideoFilter.NATURES_KISS to NaturesKissFilter.grade,
        VideoFilter.DIGICAM_FLASH to DigicamFlashFilter.grade,
        VideoFilter.PURE_JOY to PureJoyFilter.grade,
        VideoFilter.LIGHT_BOOST to LightBoostFilter.grade,
        VideoFilter.CINEMATIC_BLUES to CinematicBluesFilter.grade,
        VideoFilter.COOL_BLACK to CoolBlackFilter.grade,
        VideoFilter.PHYSIQUE_II to PhysiqueIiFilter.grade,
        VideoFilter.HOLLYWOOD_PAST to HollywoodPastFilter.grade,
        VideoFilter.DREAMY_DUSK to DreamyDuskFilter.grade,
        VideoFilter.BLUE_SPECTER to BlueSpecterFilter.grade,
        VideoFilter.DEEP_CYAN to DeepCyanFilter.grade,
        VideoFilter.MOODY_VIBE to MoodyVibeFilter.grade,
        VideoFilter.FAIRYS_MEADOW to FairysMeadowFilter.grade,
        VideoFilter.WARM_VIVID to WarmVividFilter.grade,
        VideoFilter.PINK_SPARKLE to PinkSparkleFilter.grade,
        VideoFilter.PALE_MIST to PaleMistFilter.grade,
        VideoFilter.PHYSIQUE_I to PhysiqueIFilter.grade,
        VideoFilter.DIM_LIGHT to DimLightFilter.grade,
        VideoFilter.NOIR_FLASH to NoirFlashFilter.grade,
        VideoFilter.HAUNTING_GREEN to HauntingGreenFilter.grade,
        VideoFilter.LIFT_GLOW to LiftGlowFilter.grade,
        VideoFilter.SLEEPLESS_CITY to SleeplessCityFilter.grade,
        VideoFilter.BRONZE_FILM to BronzeFilmFilter.grade,
        VideoFilter.SUNKISSED_PEACH to SunkissedPeachFilter.grade,
        VideoFilter.CYBER_MIST to CyberMistFilter.grade,
        VideoFilter.COLD_THRILLER to ColdThrillerFilter.grade,
        VideoFilter.MUTED_STREET to MutedStreetFilter.grade,
        VideoFilter.MIDNIGHT_WALK to MidnightWalkFilter.grade,
        VideoFilter.CREAM to CreamFilter.grade,
        VideoFilter.IMAGE_4 to Image4Filter.grade,
        VideoFilter.CLOUDLESS_SEA to CloudlessSeaFilter.grade,
        VideoFilter.AMERICAN_RETRO to AmericanRetroFilter.grade,
        VideoFilter.VIVID_VERSE to VividVerseFilter.grade,
        VideoFilter.LAVENDER_RAY to LavenderRayFilter.grade,
        VideoFilter.DARK_CRIMSON to DarkCrimsonFilter.grade,
        VideoFilter.PURPLE_HAZE to PurpleHazeFilter.grade,
        VideoFilter.BRONZE_HIGHLIGHT to BronzeHighlightFilter.grade,
        VideoFilter.OLD_ROMANTICS to OldRomanticsFilter.grade,
        VideoFilter.VELVET_MATTE to VelvetMatteFilter.grade,
        VideoFilter.DIGICAM_SOFT to DigicamSoftFilter.grade,
        VideoFilter.DARK_SIGIL to DarkSigilFilter.grade,
        VideoFilter.SUMMER_SODA to SummerSodaFilter.grade,
        VideoFilter.IMAGE_5 to Image5Filter.grade,
        VideoFilter.HAUNTED_CANVAS to HauntedCanvasFilter.grade,
        VideoFilter.MOMENTO to MomentoFilter.grade,
        VideoFilter.BURGUNDY to BurgundyFilter.grade,
        VideoFilter.SOMBER_PICNIC to SomberPicnicFilter.grade,
        VideoFilter.RETRO_DARLING to RetroDarlingFilter.grade,
        VideoFilter.COPPER_TWILIGHT to CopperTwilightFilter.grade,
        VideoFilter.STAGE_LIGHTS to StageLightsFilter.grade,
        VideoFilter.VIOLET_RUSH to VioletRushFilter.grade,
        VideoFilter.SPRING_SCENE to SpringSceneFilter.grade,
        VideoFilter.NIGHT_LIGHTS to NightLightsFilter.grade,
        VideoFilter.BOLD_SATURATION to BoldSaturationFilter.grade,
        VideoFilter.PINK_FUZZ to PinkFuzzFilter.grade,
        VideoFilter.IMAGE_6 to Image6Filter.grade,
        VideoFilter.WHEATFIELD_ROMANCE to WheatfieldRomanceFilter.grade,
        VideoFilter.STRANGER_DAY to StrangerDayFilter.grade,
        VideoFilter.NEWSPAPER_GLAM to NewspaperGlamFilter.grade,
        VideoFilter.ROMAN_HOLIDAY to RomanHolidayFilter.grade,
        VideoFilter.ANALOG_HORROR to AnalogHorrorFilter.grade,
        VideoFilter.PINK_CANDY to PinkCandyFilter.grade,
        VideoFilter.FATAL_GREEN to FatalGreenFilter.grade,
        VideoFilter.SHIMMERING_BANDW to ShimmeringBandwFilter.grade,
        VideoFilter.ARCHAIC_TERROR to ArchaicTerrorFilter.grade,
        VideoFilter.GHOULISH_ROT to GhoulishRotFilter.grade,
        VideoFilter.SILVER_AGE to SilverAgeFilter.grade,
        VideoFilter.GRITTY_TEXTURE to GrittyTextureFilter.grade,
        VideoFilter.IMAGE_7 to Image7Filter.grade,
        VideoFilter.GLOOMY_SEPIA to GloomySepiaFilter.grade,
        VideoFilter.FILTER_90S_GLOW to Filter90sGlowFilter.grade,
        VideoFilter.CARBON_SUMMER to CarbonSummerFilter.grade,
        VideoFilter.RETRO_STUDIO to RetroStudioFilter.grade,
        VideoFilter.VINTAGE_LAZE to VintageLazeFilter.grade,
        VideoFilter.PHOTOBOOTH to PhotoboothFilter.grade,
        VideoFilter.SULFUR_INDULGENCE to SulfurIndulgenceFilter.grade,
        VideoFilter.POLAR_SHADES to PolarShadesFilter.grade,
        VideoFilter.OLD_HONEYMOON to OldHoneymoonFilter.grade,
        VideoFilter.OLD_FLAME to OldFlameFilter.grade,
        VideoFilter.DARK_OPERA to DarkOperaFilter.grade,
        VideoFilter.WILDERNESS_NIGHT to WildernessNightFilter.grade,
        VideoFilter.IMAGE_8 to Image8Filter.grade,
        VideoFilter.OLD_FRIEND to OldFriendFilter.grade,
        VideoFilter.ANTIQUE_EXPOSURE to AntiqueExposureFilter.grade,
        VideoFilter.PAST_FAME to PastFameFilter.grade,
        VideoFilter.SILVER_EXPOSE to SilverExposeFilter.grade,
        VideoFilter.LA_CLASSIC to LaClassicFilter.grade,
        VideoFilter.WINE_WASH to WineWashFilter.grade,
        VideoFilter.SEPIA_GRAIN to SepiaGrainFilter.grade,
        VideoFilter.INTIMATE_NOSTALGIA to IntimateNostalgiaFilter.grade,
        VideoFilter.EERIE_PINK to EeriePinkFilter.grade,
        VideoFilter.OLD_PROM to OldPromFilter.grade,
        VideoFilter.PEACH_FUZZ_2 to PeachFuzz2Filter.grade,
        VideoFilter.Y2K_VOGUE to Y2kVogueFilter.grade,
        VideoFilter.SILKY_COMPLEXION to SilkyComplexionFilter.grade,
        VideoFilter.RETRO_FILM to RetroFilmFilter.grade,
        VideoFilter.RIO_NIGHTS to RioNightsFilter.grade,
        VideoFilter.FRESH_LIME to FreshLimeFilter.grade,
        VideoFilter.COLORFUL_UNIVERSE to ColorfulUniverseFilter.grade,
        VideoFilter.UNIVERSAL_SUNSET to UniversalSunsetFilter.grade,
        VideoFilter.Y2K_INTERNET_GIRL to Y2kInternetGirlFilter.grade,
        VideoFilter.UMBER_BLEACHED to UmberBleachedFilter.grade,
        VideoFilter.WICKED_FOREST to WickedForestFilter.grade,
        VideoFilter.LATERAL_SMEAR to LateralSmearFilter.grade,
        VideoFilter.SPOOKY_PORTRAIT to SpookyPortraitFilter.grade,
        VideoFilter.BRONZE_GRAY to BronzeGrayFilter.grade
    )

    /** The filters shown under one tab (search filters span all tabs). */
    fun filtersFor(tab: String): List<VideoFilter> =
        if (tab == "All" || tab == "My LUTs") {
            listOf(VideoFilter.NONE) + all.map { it.first }
        } else if (tab == "Hit") {
            listOf(VideoFilter.NONE) + all.filter { "Hit" in it.second.tabs }.map { it.first }
        } else {
            listOf(VideoFilter.NONE) + all.filter { tab in it.second.tabs }.map { it.first }
        }

    fun gradeOf(filter: VideoFilter): FilterGrade? = all.firstOrNull { it.first == filter }?.second
}
