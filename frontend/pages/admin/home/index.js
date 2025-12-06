Page({
  onShow(){
    const token = wx.getStorageSync('ADMIN_TOKEN');
    const showAdminFlag = !!wx.getStorageSync('SHOW_ADMIN');
    if(!token && !showAdminFlag){
      wx.showToast({ title:'仅管理员使用', icon:'none' });
      wx.navigateBack({ delta: 1 });
    }
  },
  goCourses(){ wx.navigateTo({ url: '/pages/admin/courses/index' }); },
  goRoster(){ wx.navigateTo({ url: '/pages/admin/roster/index' }); },
  goManageCourses(){ wx.navigateTo({ url: '/pages/admin/manage/index' }); },
  goCheckin(){ wx.navigateTo({ url: '/pages/admin/checkin/index' }); }
  ,
  logout(){
    try{ wx.removeStorageSync('ADMIN_TOKEN'); } catch(e) {}
    wx.showToast({ title:'已退出管理员', icon:'none' });
    // 返回用户主页/我的页
    wx.switchTab({ url: '/pages/profile/index' });
  }
});