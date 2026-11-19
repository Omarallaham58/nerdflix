import { configureStore } from "@reduxjs/toolkit";
import userReducer from "./slices/userSlice";
import categoryReducer from "./slices/categorySlice";
import videoReducer from "./slices/videoSlice";

export const store = configureStore({
    reducer: {
        user: userReducer,
        categories: categoryReducer,
        videos: videoReducer
    }
    
});
