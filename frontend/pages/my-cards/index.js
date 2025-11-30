// pages/my-cards/index.js
const api = require('../../utils/request.js');

Page({
  data: {
    cards: [],
    loading: false
  },
  onShow() {
    this.fetchCards();
  },
  fetchCards() {
    this.setData({ loading: true });
    api.get('/user/cards')
      .then(data => this.setData({ cards: data || [] }))
      .finally(() => this.setData({ loading: false }));
  }
})