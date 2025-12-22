Page({
  goChangePwd() {wx.navigateTo({ url: '/pages/unlock/change' });},
  goCourses(){ wx.navigateTo({ url: '/pages/admin/courses/index' }); },
  goRoster(){ wx.navigateTo({ url: '/pages/admin/roster/index' }); },
  goManageCourses(){ wx.navigateTo({ url: '/pages/admin/manage/index' }); },
  goCheckin(){ wx.navigateTo({ url: '/pages/admin/checkin/index' }); }
});