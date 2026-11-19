import React from "react";

export default function withSkeleton(WrappedComponent, SkeletonComponent) {
    return function WithSkeleton(props) {
        const { loading, ...rest } = props;
        if (loading) {
            return <SkeletonComponent />;
        }
        return <WrappedComponent {...rest} />;
    };
}