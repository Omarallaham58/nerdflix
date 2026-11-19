import api from "./axiosInstance";

export const subscribe = (categoryId) => {
  return api.post(`/api/subscriptions/subscribe?categoryId=${categoryId}`);
};

export const unsubscribe = (categoryId) => {
  return api.post(`/api/subscriptions/unsubscribe?categoryId=${categoryId}`);
};