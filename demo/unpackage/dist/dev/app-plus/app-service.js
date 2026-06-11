if (typeof Promise !== "undefined" && !Promise.prototype.finally) {
  Promise.prototype.finally = function(callback) {
    const promise = this.constructor;
    return this.then(
      (value) => promise.resolve(callback()).then(() => value),
      (reason) => promise.resolve(callback()).then(() => {
        throw reason;
      })
    );
  };
}
;
if (typeof uni !== "undefined" && uni && uni.requireGlobal) {
  const global = uni.requireGlobal();
  ArrayBuffer = global.ArrayBuffer;
  Int8Array = global.Int8Array;
  Uint8Array = global.Uint8Array;
  Uint8ClampedArray = global.Uint8ClampedArray;
  Int16Array = global.Int16Array;
  Uint16Array = global.Uint16Array;
  Int32Array = global.Int32Array;
  Uint32Array = global.Uint32Array;
  Float32Array = global.Float32Array;
  Float64Array = global.Float64Array;
  BigInt64Array = global.BigInt64Array;
  BigUint64Array = global.BigUint64Array;
}
;
if (uni.restoreGlobal) {
  uni.restoreGlobal(Vue, weex, plus, setTimeout, clearTimeout, setInterval, clearInterval);
}
(function(vue) {
  "use strict";
  function formatAppLog(type, filename, ...args) {
    if (uni.__log__) {
      uni.__log__(type, filename, ...args);
    } else {
      console[type].apply(console, [...args, filename]);
    }
  }
  const _imports_0 = "/static/1.png";
  const _imports_1 = "/static/2.png";
  const _imports_2 = "/static/3.png";
  const _imports_3 = "/static/4.png";
  const _export_sfc = (sfc, props) => {
    const target = sfc.__vccOpts || sfc;
    for (const [key, val] of props) {
      target[key] = val;
    }
    return target;
  };
  const _sfc_main$1 = {
    data() {
      return {
        videoUrl: "",
        isParsing: false,
        parseResult: null,
        isDownloading: false,
        downloadPercent: 0,
        isGettingShortLink: false,
        inputFocused: false
      };
    },
    methods: {
      handlePaste() {
        uni.getClipboardData({
          success: (res) => {
            if (res.data) {
              this.videoUrl = res.data;
              this.parseResult = null;
            } else {
              uni.showToast({
                title: "剪贴板为空",
                icon: "none"
              });
            }
          },
          fail: () => {
            uni.showToast({
              title: "获取剪贴板失败",
              icon: "none"
            });
          }
        });
      },
      handleParse() {
        if (!this.videoUrl.trim()) {
          uni.showToast({
            title: "请输入链接",
            icon: "none"
          });
          return;
        }
        this.isParsing = true;
        this.parseResult = null;
        uni.showLoading({
          title: "解析中...",
          mask: true
        });
        uni.request({
          url: "http://39.101.129.76:9999/video/share/url/parse?url=" + encodeURIComponent(this.videoUrl.trim()),
          method: "GET",
          timeout: 3e4,
          success: (res) => {
            uni.hideLoading();
            this.isParsing = false;
            if (res.statusCode === 200 && res.data) {
              if (res.data.code === 200) {
                this.parseResult = res.data;
                uni.showToast({
                  title: "解析成功",
                  icon: "success"
                });
              } else {
                uni.showToast({
                  title: res.data.msg || "解析失败",
                  icon: "none"
                });
              }
            } else {
              uni.showToast({
                title: "请求失败，请重试",
                icon: "none"
              });
            }
          },
          fail: (err) => {
            uni.hideLoading();
            this.isParsing = false;
            uni.showToast({
              title: "网络错误，请检查网络",
              icon: "none"
            });
          }
        });
      },
      handleDownload() {
        if (this.isDownloading)
          return;
        if (!this.parseResult || !this.parseResult.data || !this.parseResult.data.video_url) {
          uni.showToast({
            title: "没有可下载的视频",
            icon: "none"
          });
          return;
        }
        const videoUrl = this.parseResult.data.video_url.trim();
        const title = (this.parseResult.data.title || "video").replace(/[\/:*?"<>|]/g, "");
        const fileName = title + ".mp4";
        this.isDownloading = true;
        this.downloadPercent = 0;
        const that = this;
        let downloadPath = "_downloads/" + fileName;
        try {
          if (plus.os.name === "Android") {
            const Environment = plus.android.importClass("android.os.Environment");
            const dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            downloadPath = dir.getAbsolutePath() + "/" + fileName;
          }
        } catch (e) {
        }
        const dtask = plus.downloader.createDownload(videoUrl, {
          filename: downloadPath
        }, (d, status) => {
          that.isDownloading = false;
          if (status === 200) {
            uni.showToast({
              title: "下载完成",
              icon: "success"
            });
          } else {
            uni.showToast({
              title: "下载失败",
              icon: "none"
            });
          }
        });
        dtask.addEventListener("statechanged", function(d, status) {
          if (d.state === 3 && d.totalSize) {
            that.downloadPercent = Math.round(d.downloadedSize / d.totalSize * 100);
          }
        });
        dtask.start();
      },
      handleCopyLongLink() {
        if (!this.videoUrl.trim()) {
          uni.showToast({
            title: "没有可复制的链接",
            icon: "none"
          });
          return;
        }
        uni.setClipboardData({
          data: this.videoUrl.trim(),
          success: () => {
            uni.showToast({
              title: "长链接已复制",
              icon: "success"
            });
          },
          fail: () => {
            uni.showToast({
              title: "复制失败",
              icon: "none"
            });
          }
        });
      },
      handleCopyShortLink() {
        if (this.isGettingShortLink)
          return;
        if (!this.parseResult || !this.parseResult.data || !this.parseResult.data.video_url) {
          uni.showToast({
            title: "没有可转换的链接",
            icon: "none"
          });
          return;
        }
        this.isGettingShortLink = true;
        const StringUrl = String(this.parseResult.data.video_url).trim();
        const dataJSON = {
          "longlink": StringUrl,
          "shortlink": "",
          "expiry_delay": 0
        };
        formatAppLog("log", "at pages/index/index.vue:338", JSON.stringify(dataJSON));
        uni.request({
          url: "http://39.101.129.76:4567/api/new",
          method: "POST",
          data: dataJSON,
          timeout: 15e3,
          success: (res) => {
            this.isGettingShortLink = false;
            if (res.data) {
              const shortLink = "http://39.101.129.76:4567/" + res.data;
              uni.setClipboardData({
                data: shortLink,
                success: () => {
                  uni.showToast({
                    title: "短链接已复制",
                    icon: "success"
                  });
                },
                fail: () => {
                  uni.showToast({
                    title: "复制失败",
                    icon: "none"
                  });
                }
              });
            } else {
              uni.showToast({
                title: "获取短链接失败",
                icon: "none"
              });
            }
          },
          fail: () => {
            this.isGettingShortLink = false;
            uni.showToast({
              title: "网络错误，请检查网络",
              icon: "none"
            });
          }
        });
      },
      handleShare() {
        if (!this.parseResult || !this.parseResult.data || !this.parseResult.data.video_url) {
          uni.showToast({
            title: "没有可分享的视频",
            icon: "none"
          });
          return;
        }
        const videoUrl = this.parseResult.data.video_url.trim();
        const title = this.parseResult.data.title || "分享视频";
        const coverUrl = this.parseResult.data.cover_url || "";
        uni.share({
          provider: "weixin",
          scene: "WXSceneSession",
          type: 0,
          title,
          summary: "来自视频解析的分享",
          href: videoUrl,
          imageUrl: coverUrl,
          success: () => {
            uni.showToast({
              title: "分享成功",
              icon: "success"
            });
          },
          fail: (err) => {
            uni.showToast({
              title: "分享失败",
              icon: "none"
            });
          }
        });
      }
    }
  };
  function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
    return vue.openBlock(), vue.createElementBlock("view", { class: "container" }, [
      vue.createElementVNode("view", { class: "content-wrapper" }, [
        vue.createElementVNode("view", { class: "input-card animate-fade-in" }, [
          vue.createElementVNode(
            "view",
            {
              class: vue.normalizeClass(["input-wrapper", { "input-focused": $data.inputFocused }])
            },
            [
              vue.createElementVNode("text", { class: "input-icon" }, "🔗"),
              vue.withDirectives(vue.createElementVNode(
                "input",
                {
                  class: "url-input",
                  "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => $data.videoUrl = $event),
                  placeholder: "请输入或粘贴视频链接",
                  "placeholder-style": "color:#c0c4cc",
                  onFocus: _cache[1] || (_cache[1] = ($event) => $data.inputFocused = true),
                  onBlur: _cache[2] || (_cache[2] = ($event) => $data.inputFocused = false)
                },
                null,
                544
                /* NEED_HYDRATION, NEED_PATCH */
              ), [
                [vue.vModelText, $data.videoUrl]
              ])
            ],
            2
            /* CLASS */
          )
        ]),
        vue.createElementVNode("view", { class: "btn-group animate-fade-in" }, [
          vue.createElementVNode("button", {
            class: "btn btn-paste",
            onClick: _cache[3] || (_cache[3] = (...args) => $options.handlePaste && $options.handlePaste(...args))
          }, "粘贴链接"),
          vue.createElementVNode("button", {
            class: "btn btn-parse",
            onClick: _cache[4] || (_cache[4] = (...args) => $options.handleParse && $options.handleParse(...args)),
            disabled: !$data.videoUrl || $data.isParsing
          }, vue.toDisplayString($data.isParsing ? "解析中..." : "开始解析"), 9, ["disabled"])
        ]),
        $data.parseResult ? (vue.openBlock(), vue.createElementBlock("view", {
          key: 0,
          class: "result-section"
        }, [
          $data.parseResult.data ? (vue.openBlock(), vue.createElementBlock("view", {
            key: 0,
            class: "video-card animate-fade-in-up"
          }, [
            vue.createElementVNode("view", { class: "success-tag" }, [
              vue.createElementVNode("text", { class: "tag-text" }, "✓ 解析成功")
            ]),
            $data.parseResult.data.cover_url ? (vue.openBlock(), vue.createElementBlock("image", {
              key: 0,
              class: "cover",
              src: $data.parseResult.data.cover_url,
              mode: "aspectFill"
            }, null, 8, ["src"])) : vue.createCommentVNode("v-if", true),
            vue.createElementVNode("view", { class: "video-meta" }, [
              $data.parseResult.data.title ? (vue.openBlock(), vue.createElementBlock(
                "text",
                {
                  key: 0,
                  class: "title"
                },
                vue.toDisplayString($data.parseResult.data.title),
                1
                /* TEXT */
              )) : vue.createCommentVNode("v-if", true),
              $data.parseResult.data.author ? (vue.openBlock(), vue.createElementBlock("view", {
                key: 1,
                class: "author-row"
              }, [
                vue.createElementVNode("view", { class: "avatar-placeholder" }, [
                  vue.createElementVNode(
                    "text",
                    { class: "avatar-text" },
                    vue.toDisplayString($data.parseResult.data.author.name.charAt(0)),
                    1
                    /* TEXT */
                  )
                ]),
                vue.createElementVNode(
                  "text",
                  { class: "author" },
                  "@" + vue.toDisplayString($data.parseResult.data.author.name),
                  1
                  /* TEXT */
                )
              ])) : vue.createCommentVNode("v-if", true)
            ])
          ])) : vue.createCommentVNode("v-if", true),
          vue.createElementVNode("view", {
            class: "action-grid animate-fade-in-up",
            style: { "animation-delay": "0.1s" }
          }, [
            vue.createElementVNode(
              "view",
              {
                class: vue.normalizeClass(["action-item", { "action-item-disabled": $data.isDownloading }]),
                onClick: _cache[5] || (_cache[5] = (...args) => $options.handleDownload && $options.handleDownload(...args))
              },
              [
                vue.createElementVNode("view", { class: "action-icon-wrap action-icon-download" }, [
                  vue.createElementVNode("image", {
                    class: "action-icon-img",
                    src: _imports_0,
                    mode: "aspectFill"
                  })
                ]),
                vue.createElementVNode(
                  "text",
                  { class: "action-label" },
                  vue.toDisplayString($data.isDownloading ? "下载中" : "下载"),
                  1
                  /* TEXT */
                )
              ],
              2
              /* CLASS */
            ),
            vue.createElementVNode("view", {
              class: "action-item",
              onClick: _cache[6] || (_cache[6] = (...args) => $options.handleCopyLongLink && $options.handleCopyLongLink(...args))
            }, [
              vue.createElementVNode("view", { class: "action-icon-wrap action-icon-copy" }, [
                vue.createElementVNode("image", {
                  class: "action-icon-img",
                  src: _imports_1,
                  mode: "aspectFill"
                })
              ]),
              vue.createElementVNode("text", { class: "action-label" }, "复制长链")
            ]),
            vue.createElementVNode(
              "view",
              {
                class: vue.normalizeClass(["action-item", { "action-item-disabled": $data.isGettingShortLink }]),
                onClick: _cache[7] || (_cache[7] = (...args) => $options.handleCopyShortLink && $options.handleCopyShortLink(...args))
              },
              [
                vue.createElementVNode("view", { class: "action-icon-wrap action-icon-short" }, [
                  vue.createElementVNode("image", {
                    class: "action-icon-img",
                    src: _imports_2,
                    mode: "aspectFill"
                  })
                ]),
                vue.createElementVNode(
                  "text",
                  { class: "action-label" },
                  vue.toDisplayString($data.isGettingShortLink ? "获取中" : "复制短链"),
                  1
                  /* TEXT */
                )
              ],
              2
              /* CLASS */
            ),
            vue.createElementVNode("view", {
              class: "action-item",
              onClick: _cache[8] || (_cache[8] = (...args) => $options.handleShare && $options.handleShare(...args))
            }, [
              vue.createElementVNode("view", { class: "action-icon-wrap action-icon-share" }, [
                vue.createElementVNode("image", {
                  class: "action-icon-img",
                  src: _imports_3,
                  mode: "aspectFill"
                })
              ]),
              vue.createElementVNode("text", { class: "action-label" }, "分享")
            ])
          ]),
          $data.isDownloading ? (vue.openBlock(), vue.createElementBlock("view", {
            key: 1,
            class: "progress-section animate-fade-in-up",
            style: { "animation-delay": "0.2s" }
          }, [
            vue.createElementVNode("text", { class: "download-hint" }, "正在下载视频..."),
            vue.createElementVNode("view", { class: "progress-row" }, [
              vue.createElementVNode("view", { class: "progress-bar" }, [
                vue.createElementVNode(
                  "view",
                  {
                    class: "progress-fill",
                    style: vue.normalizeStyle({ width: $data.downloadPercent + "%" })
                  },
                  null,
                  4
                  /* STYLE */
                )
              ]),
              vue.createElementVNode(
                "text",
                { class: "progress-text" },
                vue.toDisplayString($data.downloadPercent) + "%",
                1
                /* TEXT */
              )
            ])
          ])) : vue.createCommentVNode("v-if", true)
        ])) : vue.createCommentVNode("v-if", true),
        !$data.parseResult ? (vue.openBlock(), vue.createElementBlock("view", {
          key: 1,
          class: "empty-state"
        }, [
          vue.createElementVNode("view", { class: "empty-illustration" }, [
            vue.createElementVNode("text", { class: "empty-emoji" }, "🎬")
          ]),
          vue.createElementVNode("text", { class: "empty-title" }, "粘贴短视频链接"),
          vue.createElementVNode("text", { class: "empty-desc" }, "一键解析无水印视频")
        ])) : vue.createCommentVNode("v-if", true)
      ])
    ]);
  }
  const PagesIndexIndex = /* @__PURE__ */ _export_sfc(_sfc_main$1, [["render", _sfc_render], ["__file", "D:/work/parseVideo/pages/index/index.vue"]]);
  __definePage("pages/index/index", PagesIndexIndex);
  const _sfc_main = {
    onLaunch: function() {
      formatAppLog("log", "at App.vue:4", "App Launch");
      this.requestStoragePermission();
    },
    onShow: function() {
      formatAppLog("log", "at App.vue:10", "App Show");
    },
    onHide: function() {
      formatAppLog("log", "at App.vue:13", "App Hide");
    },
    methods: {
      requestStoragePermission() {
        if (plus.os.name !== "Android")
          return;
        const Build = plus.android.importClass("android.os.Build");
        if (Build.VERSION.SDK_INT >= 30) {
          const Environment = plus.android.importClass("android.os.Environment");
          if (!Environment.isExternalStorageManager()) {
            const Intent = plus.android.importClass("android.content.Intent");
            const Settings = plus.android.importClass("android.provider.Settings");
            const Uri = plus.android.importClass("android.net.Uri");
            const intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + plus.runtime.appid));
            const main = plus.android.runtimeMainActivity();
            main.startActivity(intent);
          }
        } else {
          plus.android.requestPermissions(
            ["android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"],
            (resultObj) => {
              formatAppLog("log", "at App.vue:36", "Storage permission result:", JSON.stringify(resultObj));
            },
            (error) => {
              formatAppLog("log", "at App.vue:39", "Storage permission error:", JSON.stringify(error));
            }
          );
        }
      }
    }
  };
  const App = /* @__PURE__ */ _export_sfc(_sfc_main, [["__file", "D:/work/parseVideo/App.vue"]]);
  function createApp() {
    const app = vue.createVueApp(App);
    return {
      app
    };
  }
  const { app: __app__, Vuex: __Vuex__, Pinia: __Pinia__ } = createApp();
  uni.Vuex = __Vuex__;
  uni.Pinia = __Pinia__;
  __app__.provide("__globalStyles", __uniConfig.styles);
  __app__._component.mpType = "app";
  __app__._component.render = () => {
  };
  __app__.mount("#app");
})(Vue);
