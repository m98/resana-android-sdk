package io.resana;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NativeAd {

    public static int IMAGE = 3;
    public static int VIDEO = 4;
    public static int WEB_PAGE = 5;
    public static int GIF = 6;

    Context context;
    private long id;
    private String order;
    private String landingImageFileName;
    private String labelFileName;
    private String link;
    private String intentUri;
    private String landingUrl;
    private int landingType;
    private String apkUrl;
    private String labelUrl;
    private String labelText;
    private String backgroundColor;
    private String callForAction;
    private String shortOrdinaryText;
    private String mediumOrdinaryText;
    private String shortTitleText;
    private String mediumTitleText;
    private List<Visual> visuals;
    private String secretKey;

    NativeAd(Context context, Ad ad, String secretKey) {
        this.context = context;
        this.id = ad.data.id;
        order = ad.getId();
        this.landingImageFileName = ad.getLandingImageFileName();
        this.labelFileName = ad.getLabelFileName();
        this.link = ad.data.link;
        this.intentUri = ad.data.intent;
        if (ad.data.landing != null) {
            this.landingUrl = ad.data.landing.url;
            this.landingType = ad.data.landing.getLandingType();
        }
        if (ad.data.resanaLabel != null) {
            this.labelUrl = ad.data.resanaLabel.label;
            this.labelText = ad.data.resanaLabel.text;
        } else
            labelText = ResanaPreferences.getString(context, ResanaPreferences.PREF_RESANA_INFO_TEXT, ResanaInternal.DEFAULT_RESANA_INFO_TEXT);
        this.apkUrl = ad.getApkUrl();
        this.backgroundColor = ad.data.backgroundColor;
        this.callForAction = ad.data.callForAction;
        this.shortOrdinaryText = ((NativeDto) ad.data).texts.ordinaryText.shortText;
        this.mediumOrdinaryText = ((NativeDto) ad.data).texts.ordinaryText.mediumText;
        if (((NativeDto) ad.data).texts.titleText != null) {
            this.shortTitleText = ((NativeDto) ad.data).texts.titleText.shortText;
            this.mediumTitleText = ((NativeDto) ad.data).texts.titleText.mediumText;
        }

        if (((NativeDto) ad.data).visuals != null && ((NativeDto) ad.data).visuals.size() > 0) {
            visuals = new ArrayList<>();
            for (VisualDto visualDto : ((NativeDto) ad.data).visuals) {
                visuals.add(new NativeAd.Visual(visualDto));
            }
        }

        this.secretKey = secretKey;
    }

    long getId() {
        return id;
    }

    String getOrder() {
        return order;
    }

    boolean hasLink() {
        return link != null;
    }

    String getLink() {
        return hasLink() ? link : null;
    }

    boolean hasIntent() {
        return intentUri != null;
    }

    Intent getIntent() {
        if (hasIntent())
            return AdViewUtil.parseIntentString(intentUri);
        return null;
    }

    String getLandingUrl() {
        return landingUrl;
    }

    /**
     * this function returns type of landing
     * landings can be image, gif, video or web view.
     * you can get the url of image or gif or video or web view via {@link #getLandingUrl()}.
     *
     * @return {@link #IMAGE} for image.
     * {@link #VIDEO} for video.
     * {@link #WEB_PAGE} for web view
     */
    public int getLandingType() {
        return landingType;
    }

    boolean hasLanding() {
        return getLandingUrl() != null;
    }

    String getLabelText() {
        return labelText;
    }

    String getLabelUrl() {
        return labelUrl;
    }

    boolean hasApk() {
        return apkUrl != null;
    }

    String getApkUrl() {
        return apkUrl;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getCallForAction() {
        return callForAction;
    }

    /**
     * will return null if native ad does not have a ordinary text
     *
     * @return short version of ordinary text of a native ad
     */
    public String getShortOrdinaryText() {
        return shortOrdinaryText;
    }

    /**
     * will return null if native ad does not have a ordinary text
     *
     * @return medium version of ordinary text of a native ad
     */
    public String getMediumOrdinaryText() {
        return mediumOrdinaryText;
    }

    /**
     * will return null if native ad does not have a title text
     *
     * @return short version of title text of a native ad
     */
    public String getShortTitleText() {
        return shortTitleText;
    }

    /**
     * will return null if native ad does not have a title text
     *
     * @return medium version of title text of a native ad
     */
    public String getMediumTitleText() {
        return mediumTitleText;
    }

    public Visual getVisual() {
        int index = VisualsManager.getVisualIndex(context, this);
        return visuals.get(index);
    }


    String getSecretKey() {
        return secretKey;
    }

    String getLandingImageFileName() {
        return landingImageFileName;
    }

    String getLabelFileName() {
        return labelFileName;
    }

    public class Visual {
        Landing orgVisual;
        Landing sqVisual;
        Landing hrzLanding;

        Visual(VisualDto visualDto) {
            this.orgVisual = new Landing(visualDto.org);
            this.sqVisual = new Landing(visualDto.sq);
            this.hrzLanding = new Landing(visualDto.hrz);
        }

        public Landing getOrgVisual() {
            return orgVisual;
        }

        public Landing getSqVisual() {
            return sqVisual;
        }

        public Landing getHrzLanding() {
            return hrzLanding;
        }
    }

    public class Landing {
        private File imageFile;
        private String url;
        private String type;
        private Integer width;
        private Integer height;

        Landing(LandingDto landingDto) {
            this.imageFile = new FileManager.FileSpec(landingDto.getFileName()).getFile(context);
            this.url = landingDto.url;
            this.type = landingDto.type;
            this.width = landingDto.width;
            this.height = landingDto.height;
        }

        /**
         * get image file for a visual
         *
         * @return File image
         */
        public File getFile() {
            return imageFile;
        }

        /**
         * get url of visual
         *
         * @return
         */
        public String getUrl() {
            return url;
        }

        /**
         * this function returns landing type.
         * landings can be image, video or web view.
         * you can get the url of image or video or web view via {@link #getLandingUrl()}.
         *
         * @return {@link #IMAGE} for image.
         * {@link #VIDEO} for video.
         * {@link #WEB_PAGE} for web view
         */
        public int getType() {
            if (type.equals("image/jpeg")
                    || type.equals("image/png"))
                return IMAGE;
            if (type.equals("video/mpeg-4"))
                return VIDEO;
            if (type.equals("text/html"))
                return WEB_PAGE;
            return -1;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}