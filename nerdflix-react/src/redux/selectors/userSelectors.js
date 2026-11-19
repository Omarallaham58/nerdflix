export const isSubscribedToCategory = (state, categoryId) => {
    return state.user.subscriptions
        .some(sub => sub.categoryId === categoryId);
};
