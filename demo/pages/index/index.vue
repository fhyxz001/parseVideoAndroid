<template>
	<view class="container">
		<view class="content-wrapper">
			<view class="input-card animate-fade-in">
				<view class="input-wrapper" :class="{'input-focused': inputFocused}">
					<text class="input-icon">🔗</text>
					<input
						class="url-input"
						v-model="videoUrl"
						placeholder="请输入或粘贴视频链接"
						placeholder-style="color:#c0c4cc"
						@focus="inputFocused = true"
						@blur="inputFocused = false"
					/>
				</view>
			</view>

			<view class="btn-group animate-fade-in">
				<button class="btn btn-paste" @tap="handlePaste">粘贴链接</button>
				<button class="btn btn-parse" @tap="handleParse" :disabled="!videoUrl || isParsing">
					{{ isParsing ? '解析中...' : '开始解析' }}
				</button>
			</view>

			<view class="result-section" v-if="parseResult">
				<view class="video-card animate-fade-in-up" v-if="parseResult.data">
					<view class="success-tag">
						<text class="tag-text">✓ 解析成功</text>
					</view>
					<image class="cover" :src="parseResult.data.cover_url" mode="aspectFill" v-if="parseResult.data.cover_url" />
					<view class="video-meta">
						<text class="title" v-if="parseResult.data.title">{{ parseResult.data.title }}</text>
						<view class="author-row" v-if="parseResult.data.author">
							<view class="avatar-placeholder">
								<text class="avatar-text">{{ parseResult.data.author.name.charAt(0) }}</text>
							</view>
							<text class="author">@{{ parseResult.data.author.name }}</text>
						</view>
					</view>
				</view>

				<view class="action-grid animate-fade-in-up" style="animation-delay:0.1s">
					<view class="action-item" :class="{'action-item-disabled': isDownloading}" @tap="handleDownload">
						<view class="action-icon-wrap action-icon-download">
							<image class="action-icon-img" src="/static/1.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">{{ isDownloading ? '下载中' : '下载' }}</text>
					</view>
					<view class="action-item" @tap="handleCopyLongLink">
						<view class="action-icon-wrap action-icon-copy">
							<image class="action-icon-img" src="/static/2.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">复制长链</text>
					</view>
					<view class="action-item" :class="{'action-item-disabled': isGettingShortLink}" @tap="handleCopyShortLink">
						<view class="action-icon-wrap action-icon-short">
							<image class="action-icon-img" src="/static/3.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">{{ isGettingShortLink ? '获取中' : '复制短链' }}</text>
					</view>
					<view class="action-item" @tap="handleShare">
						<view class="action-icon-wrap action-icon-share">
							<image class="action-icon-img" src="/static/4.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">分享</text>
					</view>
				</view>

				<view class="progress-section animate-fade-in-up" v-if="isDownloading" style="animation-delay:0.2s">
					<text class="download-hint">正在下载视频...</text>
					<view class="progress-row">
						<view class="progress-bar">
							<view class="progress-fill" :style="{ width: downloadPercent + '%' }"></view>
						</view>
						<text class="progress-text">{{ downloadPercent }}%</text>
					</view>
				</view>
			</view>

			<view class="empty-state" v-if="!parseResult">
				<view class="empty-illustration">
					<text class="empty-emoji">🎬</text>
				</view>
				<text class="empty-title">粘贴短视频链接</text>
				<text class="empty-desc">一键解析无水印视频</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				videoUrl: '',
				isParsing: false,
				parseResult: null,
				isDownloading: false,
				downloadPercent: 0,
				isGettingShortLink: false,
				inputFocused: false
			}
		},
		methods: {
			handlePaste() {
				uni.getClipboardData({
					success: (res) => {
						if (res.data) {
							this.videoUrl = res.data
							this.parseResult = null
						} else {
							uni.showToast({
								title: '剪贴板为空',
								icon: 'none'
							})
						}
					},
					fail: () => {
						uni.showToast({
							title: '获取剪贴板失败',
							icon: 'none'
						})
					}
				})
			},

			handleParse() {
				if (!this.videoUrl.trim()) {
					uni.showToast({
						title: '请输入链接',
						icon: 'none'
					})
					return
				}

				this.isParsing = true
				this.parseResult = null

				uni.showLoading({
					title: '解析中...',
					mask: true
				})

				uni.request({
					url: 'http://39.101.129.76:9999/video/share/url/parse?url=' + encodeURIComponent(this.videoUrl.trim()),
					method: 'GET',
					timeout: 30000,
					success: (res) => {
						uni.hideLoading()
						this.isParsing = false

						if (res.statusCode === 200 && res.data) {
							if (res.data.code === 200) {
								this.parseResult = res.data
								uni.showToast({
									title: '解析成功',
									icon: 'success'
								})
							} else {
								uni.showToast({
									title: res.data.msg || '解析失败',
									icon: 'none'
								})
							}
						} else {
							uni.showToast({
								title: '请求失败，请重试',
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						this.isParsing = false
						uni.showToast({
							title: '网络错误，请检查网络',
							icon: 'none'
						})
					}
				})
			},

			handleDownload() {
				if (this.isDownloading) return

				if (!this.parseResult || !this.parseResult.data || !this.parseResult.data.video_url) {
					uni.showToast({
						title: '没有可下载的视频',
						icon: 'none'
					})
					return
				}

				const videoUrl = this.parseResult.data.video_url.trim()
				const title = (this.parseResult.data.title || 'video').replace(/[\/:*?"<>|]/g, '')
				const fileName = title + '.mp4'

				this.isDownloading = true
				this.downloadPercent = 0
				const that = this

				// #ifdef APP-PLUS
				let downloadPath = '_downloads/' + fileName
				try {
					if (plus.os.name === 'Android') {
						const Environment = plus.android.importClass('android.os.Environment')
						const dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
						downloadPath = dir.getAbsolutePath() + '/' + fileName
					}
				} catch (e) {}
				const dtask = plus.downloader.createDownload(videoUrl, {
					filename: downloadPath
				}, (d, status) => {
					that.isDownloading = false
					if (status === 200) {
						uni.showToast({
							title: '下载完成',
							icon: 'success'
						})
					} else {
						uni.showToast({
							title: '下载失败',
							icon: 'none'
						})
					}
				})
				dtask.addEventListener('statechanged', function(d, status) {
					if (d.state === 3 && d.totalSize) {
						that.downloadPercent = Math.round(d.downloadedSize / d.totalSize * 100)
					}
				})
				dtask.start()
				// #endif

				// #ifdef H5
				const a = document.createElement('a')
				a.href = videoUrl
				a.download = fileName
				a.style.display = 'none'
				document.body.appendChild(a)
				a.click()
				document.body.removeChild(a)
				this.isDownloading = false
				uni.showToast({
					title: '开始下载',
					icon: 'success'
				})
				// #endif

				// #ifdef MP-WEIXIN
				const downloadTask = uni.downloadFile({
					url: videoUrl,
					success: (res) => {
						if (res.statusCode === 200) {
							uni.saveVideoToPhotosAlbum({
								filePath: res.tempFilePath,
								success: () => {
									that.isDownloading = false
									uni.showToast({
										title: '已保存到相册',
										icon: 'success'
									})
								},
								fail: () => {
									that.isDownloading = false
									uni.showToast({
										title: '保存失败',
										icon: 'none'
									})
								}
							})
						} else {
							that.isDownloading = false
							uni.showToast({
								title: '下载失败',
								icon: 'none'
							})
						}
					},
					fail: () => {
						that.isDownloading = false
						uni.showToast({
							title: '下载失败',
							icon: 'none'
						})
					}
				})
				downloadTask.onProgressUpdate((res) => {
					that.downloadPercent = res.progress
				})
				// #endif
			},

			handleCopyLongLink() {
				if (!this.videoUrl.trim()) {
					uni.showToast({
						title: '没有可复制的链接',
						icon: 'none'
					})
					return
				}

				uni.setClipboardData({
					data: this.videoUrl.trim(),
					success: () => {
						uni.showToast({
							title: '长链接已复制',
							icon: 'success'
						})
					},
					fail: () => {
						uni.showToast({
							title: '复制失败',
							icon: 'none'
						})
					}
				})
			},

			handleCopyShortLink() {
				if (this.isGettingShortLink) return

				if (!this.parseResult || !this.parseResult.data || !this.parseResult.data.video_url) {
					uni.showToast({
						title: '没有可转换的链接',
						icon: 'none'
					})
					return
				}

				this.isGettingShortLink = true
				const StringUrl = String(this.parseResult.data.video_url).trim()
				const dataJSON = {
					"longlink": StringUrl,
					"shortlink": "",
					"expiry_delay": 0
				}
				console.log(JSON.stringify(dataJSON))

				uni.request({
					url: 'http://39.101.129.76:4567/api/new',
					method: 'POST',
					data: dataJSON,
					timeout: 15000,
					success: (res) => {
						this.isGettingShortLink = false
						if (res.data) {
							const shortLink = 'http://39.101.129.76:4567/' + res.data
							uni.setClipboardData({
								data: shortLink,
								success: () => {
									uni.showToast({
										title: '短链接已复制',
										icon: 'success'
									})
								},
								fail: () => {
									uni.showToast({
										title: '复制失败',
										icon: 'none'
									})
								}
							})
						} else {
							uni.showToast({
								title: '获取短链接失败',
								icon: 'none'
							})
						}
					},
					fail: () => {
						this.isGettingShortLink = false
						uni.showToast({
							title: '网络错误，请检查网络',
							icon: 'none'
						})
					}
				})
			},

			handleShare() {
				if (!this.parseResult || !this.parseResult.data || !this.parseResult.data.video_url) {
					uni.showToast({
						title: '没有可分享的视频',
						icon: 'none'
					})
					return
				}

				const videoUrl = this.parseResult.data.video_url.trim()
				const title = this.parseResult.data.title || '分享视频'
				const coverUrl = this.parseResult.data.cover_url || ''

				uni.share({
					provider: 'weixin',
					scene: 'WXSceneSession',
					type: 0,
					title: title,
					summary: '来自视频解析的分享',
					href: videoUrl,
					imageUrl: coverUrl,
					success: () => {
						uni.showToast({
							title: '分享成功',
							icon: 'success'
						})
					},
					fail: (err) => {
						uni.showToast({
							title: '分享失败',
							icon: 'none'
						})
					}
				})
			}
		}
	}
</script>

<style>
	.container {
		min-height: 100vh;
		background: linear-gradient(180deg, #f0f4ff 0%, #e8eeff 35%, #f3eeff 65%, #fef0f5 100%);
		padding: 0 0 60rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.page-header {
		width: 100%;
		padding: 24rpx 0 20rpx;
		text-align: center;
	}

	.header-subtitle {
		font-size: 26rpx;
		color: #999;
		margin-top: 12rpx;
		display: block;
	}

	.content-wrapper {
		width: 88%;
		max-width: 660rpx;
	}

	.input-card {
		background: rgba(255, 255, 255, 0.88);
		border-radius: 28rpx;
		padding: 36rpx 32rpx;
		box-shadow: 0 8rpx 40rpx rgba(100, 100, 180, 0.07);
		margin-bottom: 28rpx;
	}

	.input-wrapper {
		display: flex;
		align-items: center;
		background: #f5f6fa;
		border-radius: 18rpx;
		padding: 0 28rpx;
		height: 92rpx;
		border: 2rpx solid transparent;
		transition: all 0.3s ease;
	}

	.input-focused {
		border-color: #667eea;
		background: #fff;
		box-shadow: 0 0 0 6rpx rgba(102, 126, 234, 0.1);
	}

	.input-icon {
		font-size: 32rpx;
		margin-right: 16rpx;
		opacity: 0.45;
	}

	.url-input {
		flex: 1;
		height: 92rpx;
		font-size: 28rpx;
		color: #333;
		background: transparent;
	}

	.btn-group {
		display: flex;
		gap: 24rpx;
		margin-bottom: 48rpx;
	}

	.btn {
		flex: 1;
		height: 96rpx;
		line-height: 96rpx;
		font-size: 30rpx;
		border-radius: 22rpx;
		border: none;
		text-align: center;
		padding: 0;
		transition: all 0.2s ease;
		font-weight: 500;
	}

	.btn::after {
		border: none;
	}

	.btn:active {
		transform: scale(0.95);
	}

	.btn-paste {
		background: linear-gradient(135deg, #f8f9fd, #eef0f7);
		color: #555;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
	}

	.btn-paste:active {
		background: linear-gradient(135deg, #eef0f7, #e4e7f0);
	}

	.btn-parse {
		background: linear-gradient(135deg, #667eea, #764ba2);
		color: #fff;
		box-shadow: 0 6rpx 24rpx rgba(102, 126, 234, 0.35);
		font-weight: 600;
		letter-spacing: 2rpx;
	}

	.btn-parse:active {
		background: linear-gradient(135deg, #5a72d8, #6a4296);
	}

	.btn-parse[disabled] {
		background: linear-gradient(135deg, #c5cdf0, #d0c5e0);
		box-shadow: none;
		color: rgba(255, 255, 255, 0.7);
		transform: none;
	}

	.result-section {
		width: 100%;
	}

	.video-card {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(24px);
		-webkit-backdrop-filter: blur(24px);
		border-radius: 28rpx;
		padding: 28rpx;
		margin-bottom: 28rpx;
		box-shadow: 0 12rpx 40rpx rgba(100, 100, 180, 0.08);
		border: 1rpx solid rgba(255, 255, 255, 0.6);
		position: relative;
		overflow: hidden;
	}

	.success-tag {
		position: absolute;
		top: 28rpx;
		right: 28rpx;
		background: linear-gradient(135deg, #43e97b, #38f9d7);
		padding: 8rpx 22rpx;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 12rpx rgba(67, 233, 123, 0.25);
		z-index: 1;
	}

	.tag-text {
		font-size: 22rpx;
		color: #fff;
		font-weight: 600;
	}

	.cover {
		width: 100%;
		height: 380rpx;
		border-radius: 18rpx;
		margin-bottom: 24rpx;
		background-color: #f0f0f5;
	}

	.video-meta {
		padding: 0 8rpx;
	}

	.title {
		font-size: 30rpx;
		color: #1a1a2e;
		font-weight: 600;
		line-height: 1.6;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
		text-overflow: ellipsis;
		margin-bottom: 18rpx;
	}

	.author-row {
		display: flex;
		align-items: center;
	}

	.avatar-placeholder {
		width: 50rpx;
		height: 50rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #667eea, #764ba2);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.avatar-text {
		font-size: 24rpx;
		color: #fff;
		font-weight: 600;
	}

	.author {
		font-size: 26rpx;
		color: #999;
	}

	.action-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 20rpx;
		margin-bottom: 28rpx;
	}

	.action-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 32rpx 0 28rpx;
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		border-radius: 22rpx;
		box-shadow: 0 6rpx 24rpx rgba(100, 100, 180, 0.06);
		border: 1rpx solid rgba(255, 255, 255, 0.5);
		transition: all 0.2s ease;
	}

	.action-item:active {
		transform: scale(0.94);
	}

	.action-item-disabled {
		opacity: 0.45;
		pointer-events: none;
	}

	.action-icon-wrap {
		width: 76rpx;
		height: 76rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 14rpx;
	}

	.action-icon-download {
		background: linear-gradient(135deg, #43e97b, #38f9d7);
		box-shadow: 0 6rpx 20rpx rgba(67, 233, 123, 0.3);
	}

	.action-icon-copy {
		background: linear-gradient(135deg, #667eea, #764ba2);
		box-shadow: 0 6rpx 20rpx rgba(102, 126, 234, 0.3);
	}

	.action-icon-short {
		background: linear-gradient(135deg, #fa709a, #fee140);
		box-shadow: 0 6rpx 20rpx rgba(250, 112, 154, 0.3);
	}

	.action-icon-share {
		background: linear-gradient(135deg, #4facfe, #00f2fe);
		box-shadow: 0 6rpx 20rpx rgba(79, 172, 254, 0.3);
	}

	.action-icon-img {
		width: 44rpx;
		height: 44rpx;
		display: block;
	}

	.action-label {
		font-size: 24rpx;
		color: #555;
		font-weight: 500;
	}

	.progress-section {
		background: rgba(255, 255, 255, 0.82);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		border-radius: 22rpx;
		padding: 32rpx;
		margin-bottom: 28rpx;
		box-shadow: 0 6rpx 24rpx rgba(100, 100, 180, 0.06);
		border: 1rpx solid rgba(255, 255, 255, 0.5);
	}

	.download-hint {
		font-size: 26rpx;
		color: #667eea;
		font-weight: 500;
		margin-bottom: 24rpx;
		display: block;
	}

	.progress-row {
		display: flex;
		align-items: center;
		gap: 20rpx;
	}

	.progress-bar {
		flex: 1;
		height: 22rpx;
		background: #eef0f5;
		border-radius: 11rpx;
		overflow: hidden;
	}

	.progress-fill {
		height: 100%;
		background: linear-gradient(90deg, #667eea, #764ba2, #667eea);
		background-size: 200% 100%;
		border-radius: 11rpx;
		transition: width 0.3s ease;
		animation: progressShine 2s linear infinite;
	}

	.progress-text {
		font-size: 26rpx;
		color: #764ba2;
		font-weight: 600;
		min-width: 80rpx;
		text-align: right;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding-top: 100rpx;
	}

	.empty-illustration {
		width: 200rpx;
		height: 200rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #f0f4ff, #f3eeff);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 40rpx;
		box-shadow: 0 12rpx 40rpx rgba(102, 126, 234, 0.1);
	}

	.empty-emoji {
		font-size: 80rpx;
	}

	.empty-title {
		font-size: 32rpx;
		color: #333;
		font-weight: 600;
		margin-bottom: 12rpx;
	}

	.empty-desc {
		font-size: 26rpx;
		color: #bbb;
	}

	@keyframes fadeIn {
		from {
			opacity: 0;
		}
		to {
			opacity: 1;
		}
	}

	@keyframes fadeInUp {
		from {
			opacity: 0;
			transform: translateY(40rpx);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	@keyframes progressShine {
		0% {
			background-position: 0% 0%;
		}
		100% {
			background-position: 200% 0%;
		}
	}

	.animate-fade-in {
		animation: fadeIn 0.6s ease both;
	}

	.animate-fade-in-up {
		animation: fadeInUp 0.5s ease both;
	}
</style>
