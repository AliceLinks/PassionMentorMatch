Page({
  data: {
    latitude: 23.4701,
    longitude: 114.2832,
    checkedIn: false,
    markers: [],
    selectedLat: null,
    selectedLng: null,
    locationName: null
  },
  onReady() {
    this.mapCtx = wx.createMapContext('map')
  },
  onLoad() {
    this.ensureLocationAuth(() => {
      wx.getLocation({
        type: "gcj02",
        success: res => {
          this.setData({ latitude: res.latitude, longitude: res.longitude })
          this.getLocationName(res.latitude, res.longitude)
        },
        fail: () => {
          wx.showToast({ title: "定位失败", icon: "none" })
        }
      })
    })
  },
  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
  },
  onLocate() {
    this.ensureLocationAuth(() => {
      wx.getLocation({
        type: "gcj02",
        success: res => {
          this.setData({ latitude: res.latitude, longitude: res.longitude })
          this.getLocationName(res.latitude, res.longitude)
        },
        fail: () => {
          wx.showToast({ title: "定位失败", icon: "none" })
        }
      })
    })
  },
  ensureLocationAuth(cb) {
    wx.getSetting({
      success: s => {
        if (s.authSetting && s.authSetting["scope.userLocation"]) {
          cb && cb()
        } else {
          wx.authorize({
            scope: "scope.userLocation",
            success: () => cb && cb(),
            fail: () => {
              wx.showModal({
                content: "需要开启定位权限以显示当前位置",
                confirmText: "去设置",
                success: r => {
                  if (r.confirm) {
                    wx.openSetting({})
                  }
                }
              })
            }
          })
        }
      }
    })
  },
  onMapTap(e) {
    const lat = e && e.detail && e.detail.latitude
    const lng = e && e.detail && e.detail.longitude
    if (lat && lng) {
      const m = [{ id: 999, latitude: lat, longitude: lng, title: "选中位置" }]
      this.setData({ selectedLat: lat, selectedLng: lng, markers: m })
      this.getLocationName(lat, lng)
    } else if (this.mapCtx) {
      this.mapCtx.getCenterLocation({
        success: r => {
          const m = [{ id: 999, latitude: r.latitude, longitude: r.longitude, title: "选中位置" }]
          this.setData({ selectedLat: r.latitude, selectedLng: r.longitude, markers: m })
          this.getLocationName(r.latitude, r.longitude)
        }
      })
    }
  },
  onPickLocation() {
    this.ensureLocationAuth(() => {
      wx.chooseLocation({
        success: r => {
          const m = [{ id: 999, latitude: r.latitude, longitude: r.longitude, title: r.name || "选中位置" }]
          this.setData({ selectedLat: r.latitude, selectedLng: r.longitude, markers: m, latitude: r.latitude, longitude: r.longitude, locationName: r.name || null })
        }
      })
    })
  },
  onCheckin() {
    const lat = this.data.selectedLat || this.data.latitude
    const lng = this.data.selectedLng || this.data.longitude
    const m = [{ id: 1, latitude: lat, longitude: lng, title: "已打卡" }]
    this.setData({ checkedIn: true, markers: m })
    const list = wx.getStorageSync('checkins') || []
    list.unshift({ id: Date.now(), name: this.data.locationName || '打卡点', time: new Date().toLocaleString(), lat, lng })
    wx.setStorageSync('checkins', list)
    wx.showToast({ title: '打卡成功', icon: 'success', duration: 1200 })
    setTimeout(() => {
      this.setData({ checkedIn: false })
    }, 1200)
  },
  onShare() {
    const lat = this.data.selectedLat || this.data.latitude
    const lng = this.data.selectedLng || this.data.longitude
    const name = encodeURIComponent(this.data.locationName || '')
    const url = `/pages/feed/edit?lat=${lat}&lng=${lng}&place=${name}`
    wx.navigateTo({
      url,
      success: () => {},
      fail: () => {
        wx.showToast({ title: '无法打开发布页', icon: 'none' })
      }
    })
  },
  getLocationName(lat, lng) {
    const app = getApp()
    const key = app && app.globalData && app.globalData.mapKey
    if (!key) return
    wx.request({
      url: 'https://apis.map.qq.com/ws/geocoder/v1/',
      method: 'GET',
      data: { location: `${lat},${lng}`, key },
      success: res => {
        const name = res && res.data && res.data.result && res.data.result.address_reference && (res.data.result.address_reference.landmark_l2 && res.data.result.address_reference.landmark_l2.title || res.data.result.address_reference.landmark_l1 && res.data.result.address_reference.landmark_l1.title) || res.data.result && res.data.result.address || ''
        if (name) this.setData({ locationName: name })
      }
    })
  },
})
