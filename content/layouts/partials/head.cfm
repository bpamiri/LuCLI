<cfscript>
metaData = (isDefined("data") and isStruct(data)) ? data : {};
pageMeta = (isDefined("page") and isStruct(page) and structKeyExists(page, "meta") and isStruct(page.meta)) ? page.meta : {};

baseUrl = "";
if (isDefined("globals") and isStruct(globals) and structKeyExists(globals, "baseUrl") and len(trim("" & globals.baseUrl))) {
    baseUrl = trim("" & globals.baseUrl);
}
if (!len(baseUrl) and isDefined("config") and isStruct(config) and structKeyExists(config, "baseUrl") and len(trim("" & config.baseUrl))) {
    baseUrl = trim("" & config.baseUrl);
}
while (len(baseUrl) and right(baseUrl, 1) == "/") {
    baseUrl = left(baseUrl, len(baseUrl) - 1);
}
isDevBuild = false;
if (structKeyExists(metaData, "markspresso_dev_build")) {
    var devText = lcase(trim("" & metaData.markspresso_dev_build));
    isDevBuild = (devText == "true" or devText == "1" or devText == "yes" or devText == "on");
}

useRelativeInDev = false;
if (isDefined("config") and isStruct(config) and structKeyExists(config, "socialImages") and isStruct(config.socialImages) and structKeyExists(config.socialImages, "useRelativeInDev")) {
    var relDevText = lcase(trim("" & config.socialImages.useRelativeInDev));
    useRelativeInDev = (relDevText == "true" or relDevText == "1" or relDevText == "yes" or relDevText == "on");
}

usableBaseUrl = baseUrl;
if (len(usableBaseUrl)) {
    var localHostMatch = reFindNoCase("^https?://(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0)(:\\d+)?(/.*)?$", usableBaseUrl);
    if (localHostMatch) {
        usableBaseUrl = "";
    }
}
if (isDevBuild and useRelativeInDev) {
    usableBaseUrl = "";
}

isDocsPage = false;
if (isDefined("page") and isStruct(page) and structKeyExists(page, "relPath") and len(trim("" & page.relPath))) {
    isDocsPage = left(lCase(trim("" & page.relPath)), 5) == "docs/";
}
isPostPage = (isDefined("page") and isStruct(page) and structKeyExists(page, "collectionName") and page.collectionName == "posts");

postExcerptDescription = "";
if (isPostPage and isDefined("content") and isSimpleValue(content) and len(trim("" & content))) {
    contentHtml = "" & content;
    paragraphMatch = reFindNoCase("(?is)<p[^>]*>(.*?)</p>", contentHtml, 1, true);
    if (isStruct(paragraphMatch) and arrayLen(paragraphMatch.pos) >= 2 and paragraphMatch.pos[2] GT 0 and paragraphMatch.len[2] GT 0) {
        paragraphInner = mid(contentHtml, paragraphMatch.pos[2], paragraphMatch.len[2]);
        paragraphText = reReplace(paragraphInner, "<[^>]+>", "", "all");
        paragraphText = reReplace(paragraphText, "&nbsp;", " ", "all");
        paragraphText = reReplace(paragraphText, "\s+", " ", "all");
        paragraphText = trim(paragraphText);
        if (len(paragraphText) GT 220) {
            paragraphText = left(paragraphText, 217) & "...";
        }
        postExcerptDescription = paragraphText;
    }
}

defaultDescription = isDocsPage
    ? "LuCLI documentation for running CFML, managing Lucee servers, and configuring lucee.json."
    : "LuCLI is a focused command line interface for running CFML and managing Lucee from your terminal.";

metaTitle = "";
for (key in ["og_title", "ogTitle"]) {
    if (structKeyExists(metaData, key) and len(trim("" & metaData[key]))) {
        metaTitle = trim("" & metaData[key]);
        break;
    }
    if (structKeyExists(pageMeta, key) and len(trim("" & pageMeta[key]))) {
        metaTitle = trim("" & pageMeta[key]);
        break;
    }
}
if (!len(metaTitle)) {
    if (structKeyExists(metaData, "title") and len(trim("" & metaData.title))) {
        metaTitle = trim("" & metaData.title);
    } else if (structKeyExists(pageMeta, "title") and len(trim("" & pageMeta.title))) {
        metaTitle = trim("" & pageMeta.title);
    } else {
        metaTitle = "LuCLI";
    }
}

metaDescription = "";
for (key in ["og_description", "ogDescription"]) {
    if (structKeyExists(metaData, key) and len(trim("" & metaData[key]))) {
        metaDescription = trim("" & metaData[key]);
        break;
    }
    if (structKeyExists(pageMeta, key) and len(trim("" & pageMeta[key]))) {
        metaDescription = trim("" & pageMeta[key]);
        break;
    }
}
if (!len(metaDescription)) {
    if (structKeyExists(metaData, "description") and len(trim("" & metaData.description))) {
        metaDescription = trim("" & metaData.description);
    } else if (structKeyExists(pageMeta, "description") and len(trim("" & pageMeta.description))) {
        metaDescription = trim("" & pageMeta.description);
    } else if (structKeyExists(metaData, "subtitle") and len(trim("" & metaData.subtitle))) {
        metaDescription = trim("" & metaData.subtitle);
    } else if (structKeyExists(pageMeta, "subtitle") and len(trim("" & pageMeta.subtitle))) {
        metaDescription = trim("" & pageMeta.subtitle);
    } else if (isPostPage and len(postExcerptDescription)) {
        metaDescription = postExcerptDescription;
    } else {
        metaDescription = defaultDescription;
    }
}

metaType = "";
for (key in ["og_type", "ogType"]) {
    if (structKeyExists(metaData, key) and len(trim("" & metaData[key]))) {
        metaType = lCase(trim("" & metaData[key]));
        break;
    }
    if (structKeyExists(pageMeta, key) and len(trim("" & pageMeta[key]))) {
        metaType = lCase(trim("" & pageMeta[key]));
        break;
    }
}
if (!len(metaType)) {
    metaType = (isDefined("page") and isStruct(page) and structKeyExists(page, "collectionName") and page.collectionName == "posts")
        ? "article"
        : "website";
}

twitterCard = "";
for (key in ["twitter_card", "twitterCard"]) {
    if (structKeyExists(metaData, key) and len(trim("" & metaData[key]))) {
        twitterCard = trim("" & metaData[key]);
        break;
    }
    if (structKeyExists(pageMeta, key) and len(trim("" & pageMeta[key]))) {
        twitterCard = trim("" & pageMeta[key]);
        break;
    }
}
if (!len(twitterCard)) {
    twitterCard = "summary_large_image";
}

metaImage = "";
for (key in ["og_image", "ogImage", "image"]) {
    if (structKeyExists(metaData, key) and len(trim("" & metaData[key]))) {
        metaImage = trim("" & metaData[key]);
        break;
    }
    if (structKeyExists(pageMeta, key) and len(trim("" & pageMeta[key]))) {
        metaImage = trim("" & pageMeta[key]);
        break;
    }
}
if (!len(metaImage)) {
    metaImage = "/android-chrome-512x512.png";
}
if (!reFindNoCase("^https?://", metaImage)) {
    if (left(metaImage, 1) != "/") {
        metaImage = "/" & metaImage;
    }
    if (len(usableBaseUrl)) {
        metaImage = usableBaseUrl & metaImage;
    }
}

metaUrl = "";
for (key in ["og_url", "ogUrl", "canonical_url", "canonicalUrl"]) {
    if (structKeyExists(metaData, key) and len(trim("" & metaData[key]))) {
        metaUrl = trim("" & metaData[key]);
        break;
    }
    if (structKeyExists(pageMeta, key) and len(trim("" & pageMeta[key]))) {
        metaUrl = trim("" & pageMeta[key]);
        break;
    }
}
if (!len(metaUrl) and isDefined("page") and isStruct(page) and structKeyExists(page, "canonicalUrl") and len(trim("" & page.canonicalUrl))) {
    metaUrl = trim("" & page.canonicalUrl);
}
if (len(metaUrl) and !reFindNoCase("^https?://", metaUrl)) {
    if (left(metaUrl, 1) != "/") {
        metaUrl = "/" & metaUrl;
    }
    if (len(usableBaseUrl)) {
        metaUrl = usableBaseUrl & metaUrl;
    }
}

lineBreak = chr(10);
metaHtml = "";
metaHtml &= '<meta name="description" content="' & htmlEditFormat(metaDescription) & '">' & lineBreak;
if (len(metaUrl)) {
    metaHtml &= '<link rel="canonical" href="' & htmlEditFormat(metaUrl) & '">' & lineBreak;
}
metaHtml &= '<meta property="og:site_name" content="LuCLI">' & lineBreak;
metaHtml &= '<meta property="og:title" content="' & htmlEditFormat(metaTitle) & '">' & lineBreak;
metaHtml &= '<meta property="og:description" content="' & htmlEditFormat(metaDescription) & '">' & lineBreak;
metaHtml &= '<meta property="og:type" content="' & htmlEditFormat(metaType) & '">' & lineBreak;
if (len(metaUrl)) {
    metaHtml &= '<meta property="og:url" content="' & htmlEditFormat(metaUrl) & '">' & lineBreak;
}
if (len(metaImage)) {
    metaHtml &= '<meta property="og:image" content="' & htmlEditFormat(metaImage) & '">' & lineBreak;
}
metaHtml &= '<meta name="twitter:card" content="' & htmlEditFormat(twitterCard) & '">' & lineBreak;
metaHtml &= '<meta name="twitter:title" content="' & htmlEditFormat(metaTitle) & '">' & lineBreak;
metaHtml &= '<meta name="twitter:description" content="' & htmlEditFormat(metaDescription) & '">' & lineBreak;
if (len(metaImage)) {
    metaHtml &= '<meta name="twitter:image" content="' & htmlEditFormat(metaImage) & '">' & lineBreak;
}

writeOutput(metaHtml);
</cfscript>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<link rel="stylesheet" href="/css/main.css">
<script src="/js/main.js"></script>
<link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
<link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
<link rel="manifest" href="/site.webmanifest">