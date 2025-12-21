App({
    onLaunch() {
      this.isLogging = false;
      const u = wx.getStorageSync('userInfo') || {
        avatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
        nickname: '游客'
      };
      this.globalData.user = u;
      this.globalData.language = 'zh';
    },
  
    setLoginState(token, user) {
      if (token) {
        wx.setStorageSync('token', token);
      }
      if (user) {
        wx.setStorageSync('userInfo', user);
        this.globalData = this.globalData || {};
        this.globalData.user = user;
      }
    },
  
    globalData: { user: null, language: 'zh', mapKey: 'G42BZ-6LWLT-JVXXV-LQWMS-JCD2H-JCFJM', loginCode: '' }
  });