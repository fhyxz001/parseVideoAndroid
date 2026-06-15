<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
			// #ifdef APP-PLUS
			this.requestStoragePermission()
			// #endif
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		},
		methods: {
			requestStoragePermission() {
				if (plus.os.name !== 'Android') return

				const Build = plus.android.importClass('android.os.Build')

				if (Build.VERSION.SDK_INT >= 30) {
					const Environment = plus.android.importClass('android.os.Environment')
					if (!Environment.isExternalStorageManager()) {
						const Intent = plus.android.importClass('android.content.Intent')
						const Settings = plus.android.importClass('android.provider.Settings')
						const Uri = plus.android.importClass('android.net.Uri')
						const intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
						intent.setData(Uri.parse('package:' + plus.runtime.appid))
						const main = plus.android.runtimeMainActivity()
						main.startActivity(intent)
					}
				} else {
					plus.android.requestPermissions(
						['android.permission.WRITE_EXTERNAL_STORAGE', 'android.permission.READ_EXTERNAL_STORAGE'],
						(resultObj) => {
							console.log('Storage permission result:', JSON.stringify(resultObj))
						},
						(error) => {
							console.log('Storage permission error:', JSON.stringify(error))
						}
					)
				}
			}
		}
	}
</script>

<style>
	/* 全局 iOS 风格基础样式 */
	page {
		background-color: #f2f2f7;
		font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', sans-serif;
		-webkit-font-smoothing: antialiased;
		color: rgba(60, 60, 67, 0.9);
	}

	/* iOS 风格滚动条 */
	::-webkit-scrollbar {
		display: none;
	}
</style>
