const api = require('./utils/request.js');

App({
    onLaunch() {
      this.isLogging = false;
      this.loginPromise = null; // 复用进行中的登录承诺，避免并发
      this._loadingShown = false;
      const u = wx.getStorageSync('userInfo') || {
        avatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
        nickname: '游客'
      };
      this.globalData.user = u;
      this.globalData.language = 'zh';
    },
  
    // doLogin 方法已废弃，登录逻辑已迁移到登录页
  
    globalData: { user: null, language: 'zh', mapKey: 'G42BZ-6LWLT-JVXXV-LQWMS-JCD2H-JCFJM', loginCode: '' }
  });