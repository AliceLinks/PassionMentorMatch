Page({
  goCourses() { wx.navigateTo({ url: '/pages/admin/courses/index' }); },
  goRoster() { wx.navigateTo({ url: '/pages/admin/roster/index' }); },
  goManageCourses() { wx.navigateTo({ url: '/pages/admin/manage/index' }); },
  goCheckin() { wx.navigateTo({ url: '/pages/admin/checkin/index' }); },
  goCardIssue() { wx.navigateTo({ url: '/pages/admin/cards/index' }); },
  goContentManage() { wx.navigateTo({ url: '/pages/admin/contentManage/index' }); },
  onLogout() {
    // 清除本地 token 和解锁标记，然后跳转到解锁页
    try { wx.removeStorageSync('token'); } catch (e) { }
    try { wx.removeStorageSync('unlocked'); } catch (e) { }
    wx.showToast({ title: '已退出登录', icon: 'none' });
    setTimeout(() => { wx.redirectTo({ url: '/pages/admin/login/index' }); }, 300);
  }
});