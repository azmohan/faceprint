
package com.mediatek.settings;

import android.os.SystemProperties;

public class FeatureOption {
    public static final boolean MTK_GEMINI_SUPPORT = getValue("ro.mtk_gemini_support");
    public static final boolean MTK_DHCPV6C_WIFI = getValue("ro.mtk_dhcpv6c_wifi");
    public static final boolean MTK_WAPI_SUPPORT = getValue("ro.mtk_wapi_support");
    public static final boolean MTK_OPEN_AP_WPS_SUPPORT = getValue("mediatek.wlan.openap.wps");
    public static final boolean MTK_IPO_SUPPORT = getValue("ro.mtk_ipo_support");
    public static final boolean MTK_WIFIWPSP2P_NFC_SUPPORT =
            getValue("ro.mtk_wifiwpsp2p_nfc_support");
    public static final boolean MTK_WFD_SUPPORT = getValue("ro.mtk_wfd_support");
    public static final boolean MTK_WFD_SINK_SUPPORT = getValue("ro.mtk_wfd_sink_support");
    public static final boolean MTK_WFD_SINK_UIBC_SUPPORT =
            getValue("ro.mtk_wfd_sink_uibc_support");
    public static final boolean MTK_AUDIO_PROFILES = getValue("ro.mtk_audio_profiles");
    public static final boolean MTK_MULTISIM_RINGTONE_SUPPORT =
            getValue("ro.mtk_multisim_ringtone");
    public static final boolean MTK_PRODUCT_IS_TABLET =
            SystemProperties.get("ro.build.characteristics").equals("tablet");
    public static final boolean MTK_BESLOUDNESS_SUPPORT = getValue("ro.mtk_besloudness_support");
    public static final boolean MTK_BESSURROUND_SUPPORT = getValue("ro.mtk_bessurround_support");
    public static final boolean MTK_LOSSLESS_SUPPORT = getValue("ro.mtk_lossless_bt_audio");
    public static final boolean MTK_NFC_ADDON_SUPPORT = getValue("ro.mtk_nfc_addon_support");
    public static final boolean MTK_BEAM_PLUS_SUPPORT = getValue("ro.mtk_beam_plus_support");

    public static final boolean MTK_TETHERING_EEM_SUPPORT =
            getValue("ro.mtk_tethering_eem_support");
    public static final boolean MTK_TETHERINGIPV6_SUPPORT =
            getValue("ro.mtk_tetheringipv6_support");

    public static final boolean MTK_SYSTEM_UPDATE_SUPPORT =
         getValue("ro.mtk_system_update_support");
    public static final boolean MTK_SCOMO_ENTRY = getValue("ro.mtk_scomo_entry");
    public static final boolean MTK_MDM_SCOMO = getValue("ro.mtk_mdm_scomo");
    public static final boolean MTK_FOTA_ENTRY = getValue("ro.mtk_fota_entry");
    public static final boolean MTK_MDM_FUMO = getValue("ro.mtk_mdm_fumo");
    public static final boolean MTK_DRM_APP = getValue("ro.mtk_oma_drm_support");
    public static final boolean MTK_MIRAVISION_SETTING_SUPPORT =
            getValue("ro.mtk_miravision_support");
    public static final boolean MTK_CLEARMOTION_SUPPORT = getValue("ro.mtk_clearmotion_support");

    public static final boolean MTK_AGPS_APP = getValue("ro.mtk_agps_app");
    public static final boolean MTK_OMACP_SUPPORT = getValue("ro.mtk_omacp_support");
    public static final boolean MTK_GPS_SUPPORT = getValue("ro.mtk_gps_support");
    public static final boolean MTK_VOICE_UI_SUPPORT = getValue("ro.mtk_voice_ui_support");
    public static final boolean MTK_BG_POWER_SAVING_SUPPORT =
            getValue("ro.mtk_bg_power_saving_support");
    public static final boolean MTK_BG_POWER_SAVING_UI_SUPPORT =
            getValue("ro.mtk_bg_power_saving_ui");
    public static final boolean MTK_GMO_RAM_OPTIMIZE = getValue("ro.mtk_gmo_ram_optimize");
    /// M: for Telephony settings @{
    public static final boolean MTK_VOLTE_SUPPORT = getValue("ro.mtk_volte_support");
    // for C2K
    public static final boolean PURE_AP_USE_EXTERNAL_MODEM =
        getValue("ro.pure_ap_use_external_modem");
    public static final boolean MTK_C2K_SUPPORT = getValue("ro.mtk_c2k_support");
    public static final boolean MTK_SVLTE_SUPPORT = getValue("ro.mtk_svlte_support");
    /// @}
    public static final boolean MTK_VOICE_UNLOCK_SUPPORT = getValue("ro.mtk_voice_unlock_support");

    /// M: Add for CT 6M. @ {
    public static final boolean MTK_CT6M_SUPPORT = getValue("ro.ct6m_support");
    /// @ }
    public static final boolean MTK_RUNTIME_PERMISSION_SUPPORT =
        getValue("ro.mtk_runtime_permission");

    /// M: [C2K solution 1.5]
    public static final boolean MTK_C2K_SLOT2_SUPPORT = getValue("ro.mtk.c2k.slot2.support");
    public static final boolean MTK_LTE_SUPPORT = getValue("ro.mtk_lte_support");
    public static final boolean MTK_DISABLE_CAPABILITY_SWITCH =
            getValue("ro.mtk_disable_cap_switch");
    /// @}

    /// M: [A1] @{
    public static final boolean MTK_A1_FEATURE = getValue("ro.mtk_a1_feature");

     //Added by mingjun for freeme wifi
    public static final boolean TYD_MMI_FREEME_WIFI =  getValue("ro.tyd_mmi_freeme_wifi");
    //end


    public static final boolean MTK_AAL_SUPPORT = getValue("ro.mtk_aal_support");
    public static final boolean TYD_HOLIDAY_MODE_SUPPORT = getValue("ro.tyd_holiday_mode_support");
    public static final boolean TYD_VISITOR_MODE_SUPPORT = getValue("ro.tyd_visitor_mode_support");
    public static final boolean FREEME_NON_TOUCH_OPERATION_SUPPORT = getValue("ro.tyd_non_touch_operation");
    public static final boolean TYD_SMART_DIAL_SUPPORT = getValue("ro.tyd_smart_dial_support");
    public static final boolean TYD_TP_GLOVE_SUPPORT = getValue("ro.freeme.tp_glove_support");
    public static final boolean TYD_SHAKE_OPEN_APP_SUPPORT = getValue("ro.tyd_shake_open_app_support");
    public static final boolean TYD_CPU_INFO_BITS = getValue("ro.tyd_cpu_info_bits");
	public static final boolean FREEME_CPU_INFO_FAKE = getValue("ro.freeme.cpu_info_fake");

    //*/droi.duanzhiquan, 20161221,reverse dial silent option
    public static final boolean FREEME_REVERSE_DIAL_SILENT = getValue("ro.freeme.reverse_dial_silent");
    //*/

    //*/freeme.gejun, 20160607,smart dial answer option
    public static final boolean FREEME_SMART_DIAL_ANSWER = getValue("ro.freeme.smart_dial_answer");
    //*/
    
    //*/freeme. xiaocui, 20160622. add floattask switch
    public static final boolean FREEME_FLOATTASK_SUPPORT = getValue("ro.freeme_floattask");
    //*/

    // M:WFC @ {
    public static final boolean MTK_WFC_SUPPORT = getValue("ro.mtk_wfc_support");
    // @}

    public static int getExternalModemSlot() {
        return SystemProperties.getInt("ril.external.md", 0) - 1;
    }
    //*/
    /// @}

    /// M: Add for EMMC and FLT. @ {
    public static final boolean MTK_EMMC_SUPPORT = getValue("ro.mtk_emmc_support");
    public static final boolean MTK_CACHE_MERGE_SUPPORT = getValue("ro.mtk_cache_merge_support");
    public static final boolean MTK_NAND_FTL_SUPPORT = getValue("ro.mtk_nand_ftl_support");
    /// @}

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
