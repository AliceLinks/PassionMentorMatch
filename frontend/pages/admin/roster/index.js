const api = require('../../../utils/admin-request.js');
const app = getApp();
Page({
  data: {
    list: [],
    loading: false
  },
  onLoad(query) {
    if(query && query.courseId){
      this.getList(query.courseId);
    }
  },
  getList(courseId) {
    this.setData({ loading: true });
    api.get('/reservations/course/' + courseId).then(res => {
      // 后端返回 { list: [...], total: n }
      this.setData({ list: res.list || [] });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});