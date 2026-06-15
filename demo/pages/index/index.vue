<template>
	<view class="container">
		<!-- 顶部装饰光晕 -->
		<view class="bg-orb bg-orb-1"></view>
		<view class="bg-orb bg-orb-2"></view>
		<view class="bg-orb bg-orb-3"></view>

		<view class="content-wrapper">
			<!-- 输入区域 - 玻璃卡片 -->
			<view class="glass-card input-card animate-fade-in">
				<view class="card-label">
					<text class="card-label-icon">🔗</text>
					<text class="card-label-text">视频链接</text>
				</view>
				<view class="input-wrapper" :class="{'input-focused': inputFocused}">
					<input
						class="url-input"
						v-model="videoUrl"
						placeholder="请输入或粘贴视频链接"
						placeholder-style="color:rgba(60,60,67,0.3)"
						@focus="inputFocused = true"
						@blur="inputFocused = false"
					/>
					<view class="input-clear" v-if="videoUrl" @tap="videoUrl = ''">
						<text class="clear-icon">✕</text>
					</view>
				</view>
			</view>

			<!-- 操作按钮组 -->
			<view class="btn-group animate-fade-in">
				<button class="btn btn-paste" @tap="handlePaste">
					<text class="btn-icon">📋</text>
					<text class="btn-text">粘贴链接</text>
				</button>
				<button class="btn btn-parse" @tap="handleParse" :disabled="!videoUrl || isParsing">
					<text class="btn-text">{{ isParsing ? '解析中...' : '开始解析' }}</text>
				</button>
			</view>

			<!-- 解析结果 -->
			<view class="result-section" v-if="parseResult">
				<!-- 视频信息卡片 -->
				<view class="glass-card video-card animate-fade-in-up" v-if="parseResult.data">
					<view class="success-badge">
						<text class="badge-icon">✓</text>
						<text class="badge-text">解析成功</text>
					</view>
					<view class="cover-wrapper" v-if="parseResult.data.cover_url">
						<image class="cover" :src="parseResult.data.cover_url" mode="aspectFill" />
						<view class="cover-overlay"></view>
					</view>
					<view class="video-meta">
						<text class="title" v-if="parseResult.data.title">{{ parseResult.data.title }}</text>
						<view class="author-row" v-if="parseResult.data.author">
							<view class="avatar">
								<text class="avatar-text">{{ parseResult.data.author.name.charAt(0) }}</text>
							</view>
							<text class="author">@{{ parseResult.data.author.name }}</text>
						</view>
					</view>
				</view>

				<!-- 操作网格 -->
				<view class="action-grid animate-fade-in-up" style="animation-delay:0.1s">
					<view class="glass-card action-item" :class="{'action-disabled': isDownloading}" @tap="handleDownload">
						<view class="action-icon-bg action-icon-download">
							<image class="action-icon-img" src="/static/1.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">{{ isDownloading ? '下载中' : '下载' }}</text>
					</view>
					<view class="glass-card action-item" @tap="handleCopyLongLink">
						<view class="action-icon-bg action-icon-copy">
							<image class="action-icon-img" src="/static/2.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">复制长链</text>
					</view>
					<view class="glass-card action-item" :class="{'action-disabled': isGettingShortLink}" @tap="handleCopyShortLink">
						<view class="action-icon-bg action-icon-short">
							<image class="action-icon-img" src="/static/3.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">{{ isGettingShortLink ? '获取中' : '复制短链' }}</text>
					</view>
					<view class="glass-card action-item" @tap="handleShare">
						<view class="action-icon-bg action-icon-share">
							<image class="action-icon-img" src="/static/4.png" mode="aspectFill"></image>
						</view>
						<text class="action-label">分享</text>
					</view>
				</view>

				<!-- 下载进度 -->
				<view class="glass-card progress-card animate-fade-in-up" v-if="isDownloading" style="animation-delay:0.2s">
					<view class="progress-header">
						<text class="progress-title">正在下载视频</text>
						<text class="progress-percent">{{ downloadPercent }}%</text>
					</view>
					<view class="progress-track">
						<view class="progress-fill" :style="{ width: downloadPercent + '%' }"></view>
					</view>
				</view>
			</view>

			<!-- 空状态 -->
			<view class="empty-state" v-if="!parseResult">
				<view class="empty-glass animate-fade-in">
					<text class="empty-emoji">🎬</text>
				</view>
				<text class="empty-title animate-fade-in">粘贴短视频链接</text>
				<text class="empty-desc animate-fade-in">一键解析无水印视频</text>
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
	/* ===== iOS Design System Tokens ===== */
	:root {
		--ios-blue: #007AFF;
		--ios-blue-light: rgba(0, 122, 255, 0.12);
		--ios-green: #34C759;
		--ios-pink: #FF2D55;
		--ios-orange: #FF9500;
		--ios-teal: #5AC8FA;
		--ios-purple: #AF52DE;
		--ios-label: rgba(60, 60, 67, 0.9);
		--ios-label-secondary: rgba(60, 60, 67, 0.6);
		--ios-label-tertiary: rgba(60, 60, 67, 0.3);
		--ios-separator: rgba(60, 60, 67, 0.12);
		--ios-fill: rgba(120, 120, 128, 0.2);
		--ios-glass-bg: rgba(255, 255, 255, 0.72);
		--ios-glass-bg-heavy: rgba(255, 255, 255, 0.85);
		--ios-glass-border: rgba(255, 255, 255, 0.5);
		--ios-radius-xl: 28rpx;
		--ios-radius-lg: 22rpx;
		--ios-radius-md: 16rpx;
		--ios-radius-sm: 12rpx;
	}

	/* ===== Container & Background ===== */
	.container {
		min-height: 100vh;
		background: #f2f2f7;
		padding: 0 0 env(safe-area-inset-bottom, 40rpx);
		position: relative;
		overflow: hidden;
		font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', sans-serif;
	}

	/* 背景装饰光晕 - 营造 iOS 氛围 */
	.bg-orb {
		position: fixed;
		border-radius: 50%;
		filter: blur(120rpx);
		opacity: 0.5;
		pointer-events: none;
		z-index: 0;
	}

	.bg-orb-1 {
		width: 600rpx;
		height: 600rpx;
		background: rgba(0, 122, 255, 0.15);
		top: -200rpx;
		right: -150rpx;
	}

	.bg-orb-2 {
		width: 500rpx;
		height: 500rpx;
		background: rgba(175, 82, 222, 0.12);
		top: 400rpx;
		left: -200rpx;
	}

	.bg-orb-3 {
		width: 450rpx;
		height: 450rpx;
		background: rgba(52, 199, 89, 0.1);
		bottom: -100rpx;
		right: -100rpx;
	}

	/* ===== Content Layout ===== */
	.content-wrapper {
		position: relative;
		z-index: 1;
		width: 92%;
		max-width: 700rpx;
		margin: 0 auto;
		padding-top: 32rpx;
	}

	/* ===== Glass Card Base ===== */
	.glass-card {
		background: var(--ios-glass-bg);
		backdrop-filter: blur(40px) saturate(180%);
		-webkit-backdrop-filter: blur(40px) saturate(180%);
		border-radius: var(--ios-radius-xl);
		border: 1rpx solid var(--ios-glass-border);
		box-shadow:
			0 2rpx 8rpx rgba(0, 0, 0, 0.04),
			0 8rpx 32rpx rgba(0, 0, 0, 0.06);
	}

	/* ===== Input Card ===== */
	.input-card {
		padding: 32rpx;
		margin-bottom: 28rpx;
	}

	.card-label {
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
	}

	.card-label-icon {
		font-size: 28rpx;
		margin-right: 10rpx;
	}

	.card-label-text {
		font-size: 26rpx;
		color: var(--ios-label-secondary);
		font-weight: 500;
		letter-spacing: 0.5rpx;
	}

	.input-wrapper {
		display: flex;
		align-items: center;
		background: rgba(120, 120, 128, 0.08);
		border-radius: var(--ios-radius-md);
		padding: 0 24rpx;
		height: 96rpx;
		border: 2rpx solid transparent;
		transition: all 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
	}

	.input-focused {
		border-color: var(--ios-blue);
		background: rgba(0, 122, 255, 0.04);
		box-shadow: 0 0 0 6rpx rgba(0, 122, 255, 0.12);
	}

	.url-input {
		flex: 1;
		height: 96rpx;
		font-size: 30rpx;
		color: var(--ios-label);
		background: transparent;
		letter-spacing: 0.3rpx;
	}

	.input-clear {
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		background: rgba(120, 120, 128, 0.2);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		margin-left: 12rpx;
		transition: all 0.2s ease;
	}

	.input-clear:active {
		transform: scale(0.85);
		background: rgba(120, 120, 128, 0.35);
	}

	.clear-icon {
		font-size: 22rpx;
		color: rgba(60, 60, 67, 0.6);
	}

	/* ===== Buttons ===== */
	.btn-group {
		display: flex;
		gap: 20rpx;
		margin-bottom: 40rpx;
	}

	.btn {
		flex: 1;
		height: 100rpx;
		border-radius: var(--ios-radius-lg);
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0;
		transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
		font-weight: 600;
		letter-spacing: 0.5rpx;
	}

	.btn::after {
		border: none;
	}

	.btn:active {
		transform: scale(0.96);
		opacity: 0.85;
	}

	.btn-icon {
		font-size: 30rpx;
		margin-right: 8rpx;
	}

	.btn-text {
		font-size: 30rpx;
	}

	.btn-paste {
		background: var(--ios-glass-bg-heavy);
		backdrop-filter: blur(20px);
		-webkit-backdrop-filter: blur(20px);
		color: var(--ios-label);
		border: 1rpx solid var(--ios-glass-border);
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
	}

	.btn-paste:active {
		background: rgba(255, 255, 255, 0.65);
	}

	.btn-parse {
		background: var(--ios-blue);
		color: #fff;
		box-shadow: 0 6rpx 24rpx rgba(0, 122, 255, 0.35);
	}

	.btn-parse:active {
		background: #0066D6;
	}

	.btn-parse[disabled] {
		background: rgba(0, 122, 255, 0.3);
		box-shadow: none;
		color: rgba(255, 255, 255, 0.6);
		transform: none;
	}

	/* ===== Result Section ===== */
	.result-section {
		width: 100%;
	}

	/* ===== Video Card ===== */
	.video-card {
		padding: 28rpx;
		margin-bottom: 24rpx;
		position: relative;
		overflow: hidden;
	}

	.success-badge {
		position: absolute;
		top: 24rpx;
		right: 24rpx;
		display: flex;
		align-items: center;
		background: rgba(52, 199, 89, 0.15);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		padding: 8rpx 20rpx;
		border-radius: 40rpx;
		z-index: 2;
		border: 1rpx solid rgba(52, 199, 89, 0.2);
	}

	.badge-icon {
		font-size: 22rpx;
		color: var(--ios-green);
		font-weight: 700;
		margin-right: 6rpx;
	}

	.badge-text {
		font-size: 22rpx;
		color: var(--ios-green);
		font-weight: 600;
	}

	.cover-wrapper {
		position: relative;
		border-radius: var(--ios-radius-lg);
		overflow: hidden;
		margin-bottom: 24rpx;
	}

	.cover {
		width: 100%;
		height: 400rpx;
		display: block;
		background-color: rgba(120, 120, 128, 0.08);
	}

	.cover-overlay {
		position: absolute;
		bottom: 0;
		left: 0;
		right: 0;
		height: 120rpx;
		background: linear-gradient(transparent, rgba(0, 0, 0, 0.08));
		pointer-events: none;
	}

	.video-meta {
		padding: 0 8rpx;
	}

	.title {
		font-size: 32rpx;
		color: var(--ios-label);
		font-weight: 600;
		line-height: 1.55;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
		text-overflow: ellipsis;
		margin-bottom: 20rpx;
		letter-spacing: 0.3rpx;
	}

	.author-row {
		display: flex;
		align-items: center;
	}

	.avatar {
		width: 52rpx;
		height: 52rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, var(--ios-blue), var(--ios-purple));
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
		color: var(--ios-label-secondary);
	}

	/* ===== Action Grid ===== */
	.action-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 20rpx;
		margin-bottom: 24rpx;
	}

	.action-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 36rpx 0 30rpx;
		transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
	}

	.action-item:active {
		transform: scale(0.94);
	}

	.action-disabled {
		opacity: 0.4;
		pointer-events: none;
	}

	.action-icon-bg {
		width: 84rpx;
		height: 84rpx;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 16rpx;
		transition: transform 0.2s ease;
	}

	.action-item:active .action-icon-bg {
		transform: scale(0.9);
	}

	.action-icon-download {
		background: linear-gradient(135deg, #34C759, #30D158);
		box-shadow: 0 6rpx 20rpx rgba(52, 199, 89, 0.3);
	}

	.action-icon-copy {
		background: linear-gradient(135deg, #007AFF, #5856D6);
		box-shadow: 0 6rpx 20rpx rgba(0, 122, 255, 0.3);
	}

	.action-icon-short {
		background: linear-gradient(135deg, #FF2D55, #FF6482);
		box-shadow: 0 6rpx 20rpx rgba(255, 45, 85, 0.3);
	}

	.action-icon-share {
		background: linear-gradient(135deg, #5AC8FA, #007AFF);
		box-shadow: 0 6rpx 20rpx rgba(90, 200, 250, 0.3);
	}

	.action-icon-img {
		width: 44rpx;
		height: 44rpx;
		display: block;
	}

	.action-label {
		font-size: 24rpx;
		color: var(--ios-label-secondary);
		font-weight: 500;
		letter-spacing: 0.3rpx;
	}

	/* ===== Progress Card ===== */
	.progress-card {
		padding: 32rpx;
		margin-bottom: 24rpx;
	}

	.progress-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 24rpx;
	}

	.progress-title {
		font-size: 28rpx;
		color: var(--ios-label);
		font-weight: 500;
	}

	.progress-percent {
		font-size: 28rpx;
		color: var(--ios-blue);
		font-weight: 700;
		font-variant-numeric: tabular-nums;
	}

	.progress-track {
		height: 12rpx;
		background: rgba(120, 120, 128, 0.12);
		border-radius: 6rpx;
		overflow: hidden;
	}

	.progress-fill {
		height: 100%;
		background: linear-gradient(90deg, var(--ios-blue), #5856D6);
		border-radius: 6rpx;
		transition: width 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
	}

	/* ===== Empty State ===== */
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding-top: 120rpx;
	}

	.empty-glass {
		width: 200rpx;
		height: 200rpx;
		border-radius: 56rpx;
		background: var(--ios-glass-bg);
		backdrop-filter: blur(40px) saturate(180%);
		-webkit-backdrop-filter: blur(40px) saturate(180%);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 44rpx;
		border: 1rpx solid var(--ios-glass-border);
		box-shadow:
			0 4rpx 16rpx rgba(0, 0, 0, 0.04),
			0 16rpx 48rpx rgba(0, 0, 0, 0.06);
	}

	.empty-emoji {
		font-size: 80rpx;
	}

	.empty-title {
		font-size: 34rpx;
		color: var(--ios-label);
		font-weight: 600;
		margin-bottom: 12rpx;
		letter-spacing: 0.5rpx;
	}

	.empty-desc {
		font-size: 28rpx;
		color: var(--ios-label-tertiary);
		letter-spacing: 0.3rpx;
	}

	/* ===== Animations ===== */
	@keyframes fadeIn {
		from { opacity: 0; }
		to { opacity: 1; }
	}

	@keyframes fadeInUp {
		from {
			opacity: 0;
			transform: translateY(32rpx);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	.animate-fade-in {
		animation: fadeIn 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
	}

	.animate-fade-in-up {
		animation: fadeInUp 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
	}
</style>
