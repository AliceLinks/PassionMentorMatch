const api = require('../../../utils/request.js');

Page({
  data: {
    roadmapImg: '',
    courseIntro: ''
  },
  onLoad() {
    this.fetchConfig();
  },
  fetchConfig() {
    api.get('/admin/config').then(cfg => {
      this.setData({
        roadmapImg: cfg.roadmapImg || '',
        courseIntro: cfg.courseIntro || ''
      });
    });
  },
  chooseRoadmapImg() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: res => {
        const filePath = res.tempFilePaths[0];
        // 保证上传接口始终带 /api 前缀
        let uploadUrl = api.getBaseUrl() + '/admin/upload';
        if (!/\/api\//.test(uploadUrl)) {
          uploadUrl = api.getBaseUrl().replace(/\/$/, '') + '/api/admin/upload';
        }
        console.log('实际上传url:', uploadUrl);
        wx.uploadFile({
          url: uploadUrl,
          filePath,
          name: 'file',
          header: api.getUploadHeaders(),
          success: upRes => {
            const data = JSON.parse(upRes.data);
            console.log('上传接口返回:', data);
            if (data.code === 200 && data.data && data.data.url) {
              // 保存图片url到配置
              api.post('/admin/config/roadmap', { url: data.data.url }).then((saveRes) => {
                wx.showToast({ title: '已更新', icon: 'success' });
                this.setData({ roadmapImg: data.data.url }, () => {
                  console.log('setData 后 roadmapImg:', this.data.roadmapImg);
                });
                console.log('保存图片url到配置接口返回:', saveRes);
              });
            } else {
              wx.showToast({ title: '上传失败', icon: 'none' });
              console.log('上传失败，返回数据:', data);
            }
          },
          fail: (err) => {
            wx.showToast({ title: '上传失败', icon: 'none' });
            console.log('wx.uploadFile fail:', err);
          }
        });
      }
    });
  },
  onCourseIntroInput(e) {
    this.setData({ courseIntro: e.detail.value });
  },
  saveCourseIntro() {
    const link = this.data.courseIntro.trim();
    if (!/^https?:\/\//.test(link)) {
      wx.showToast({ title: '请输入有效链接', icon: 'none' });
      return;
    }
    api.post('/admin/config/course-intro', { courseIntro: link }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
    });
  }
});

// utils/request.js 需补充 getBaseUrl/getUploadHeaders 方法
